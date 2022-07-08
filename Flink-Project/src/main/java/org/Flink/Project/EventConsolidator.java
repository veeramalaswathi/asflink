package com.sling.dacoe.jobs;

import com.sling.dacoe.operators.*;
import com.sling.dacoe.schema.Error;
import com.sling.dacoe.schema.*;
import com.sling.dacoe.sinks.ZeusKafkaSink;
import com.sling.dacoe.source.FileInitialMap;
import com.sling.dacoe.source.KafkaConfig;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineFormat;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.BasePathBucketAssigner;
import org.apache.flink.util.OutputTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.Properties;

import static com.sling.dacoe.utils.ParameterUtil.fromApplicationProperties;

public class EventConsolidator {

    private static final Logger logger = LoggerFactory.getLogger(EventConsolidator.class);
    public final static TypeInformation<ZeusEvent> zeusEventTypeInfo = Types.POJO(ZeusEvent.class);
    private static ParameterTool parameterTool;
    private static boolean isLocal = false;

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        if (args.length > 0 && args[0].equalsIgnoreCase("local")) {
            logger.info("Running in local machine");
            parameterTool = loadLocalEnvironmentProperties();
            isLocal = true;
        } else {
            logger.info("Running in EMR");
            parameterTool = ParameterTool.fromArgs(args);
        }
        setStreamExecutionEnvironmentConfig(env);
        env.getConfig().disableGenericTypes();
        env.getConfig().registerPojoType(zeusEventTypeInfo.getTypeClass());

        env.getConfig().registerPojoType(Bitrate.class);
        env.getConfig().registerPojoType(CDN.class);
        env.getConfig().registerPojoType(Error.class);
        env.getConfig().registerPojoType(Asset.class);
        env.getConfig().registerPojoType(ZeusPlayback.class);
        env.getConfig().registerPojoType(PlaybackStatus.class);

        DataStream<Tuple1<String>> sourceData;

        if (isLocal) {
            String path = parameterTool.get("input-path");
            final FileSource<String> source = FileSource.forRecordStreamFormat(new TextLineFormat(), new Path(path))
                    .processStaticFileSet()
                    .build();

            //WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofMinutes(10))
              //      .withIdleness(Duration.ofMinutes(1));
            DataStream<String> dataStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "file-source");

            sourceData = dataStream.map(new FileInitialMap());

        } else {
            sourceData = KafkaConfig.createKafkaSource(env, parameterTool);
        }

        // Parse Data from Source
        DataStream<ZeusEvent> parsedData = sourceData
                .map(new ZeusRawEventsMapper())
                .filter(new ZeusFilterFunction(parameterTool.getLong("events-threshold-count",100)))
                .setParallelism(parameterTool.getInt("zeus-kafka-source-parallelism"))
                .name("Zeus Kafka Source");


        final OutputTag<ZeusEvent> lateEvents = new OutputTag<>("lateEvents", TypeInformation.of(ZeusEvent.class));
        //Create a Session Window for One Minute (group all playback related information)
        SingleOutputStreamOperator<ZeusPlayback> aggregateData = parsedData
                .assignTimestampsAndWatermarks(WatermarkStrategy.<ZeusEvent>forBoundedOutOfOrderness(Duration.ofMinutes(1))
                        .withTimestampAssigner((event, l) -> (event.getReceivedAtInUtcMs() / 60000) * 60000)
                        .withIdleness(Duration.ofSeconds(60)))
                .keyBy(new ZeusRawKeyByFunction())
                .process(new ZeusProcessFunction(Time.minutes(1),Time.minutes(15)))
                .setParallelism(parameterTool.getInt("zeus-aggregator-parallelism"))
                .name("Zeus Aggregator");

       /* SingleOutputStreamOperator<ZeusAppRunStat> appStatsData = parsedData
                .assignTimestampsAndWatermarks(WatermarkStrategy.<ZeusEvent>forBoundedOutOfOrderness(Duration.ofMinutes(1))
                        .withTimestampAssigner((event, l) -> (event.getReceivedAtInUtcMs() / 60000) * 60000)
                        .withIdleness(Duration.ofSeconds(60)))
                .keyBy(new ZeusRawKeyByFunction())
                .process(new ZeusAppStatProcessFunction(Time.minutes(1),Time.minutes(15)))
                .setParallelism(parameterTool.getInt("zeus-aggregator-parallelism"))
                .name("Zeus App Open Vs Crash Aggregator");*/

        if (isLocal) {

            SingleOutputStreamOperator<String> finalStream = aggregateData.map(new ZeusLocalMetricsMapFunction());
            Path path = new Path(parameterTool.get("output-path"));
           /* FileSink<String> fileSink = FileSink
                    .forRowFormat(path, new SimpleStringEncoder<String>())
                    .withBucketAssigner(new BasePathBucketAssigner<>())
                    .build();
            finalStream.sinkTo(fileSink);*/

            /*SingleOutputStreamOperator<String> finalAppStatStream = appStatsData.map(new ZeusLocalAppStatsMapFunction());

            Path appStatspath = new Path(parameterTool.get("output-path"));
            FileSink<String> appStatFileSink = FileSink
                    .forRowFormat(appStatspath, new SimpleStringEncoder<String>())
                    .withBucketAssigner(new BasePathBucketAssigner<>())
                    .build();
            finalAppStatStream.sinkTo(appStatFileSink);*/

        } else {
            SingleOutputStreamOperator<Tuple2<String, String>> finalStream =
                    aggregateData.map(new ZeusMetricsMapFunction())
                            .setParallelism(parameterTool.getInt("zeus-kafka-sink-parallelism"))
                            .name("Zeus Kafka Sink Mapper");
            // Kafka Output for Correct Data
            KafkaSink<Tuple2<String, String>> producer = ZeusKafkaSink.createKafkaSink(parameterTool.get("output-topic"), parameterTool);
            //Emit Playback Session Information every minute.
            finalStream.sinkTo(producer)
                    .setParallelism(parameterTool.getInt("zeus-kafka-sink-parallelism"))
                    .name("Zeus Kafka Sink");
        }

        SingleOutputStreamOperator<String> lateEventStream = aggregateData.getSideOutput(lateEvents)
                .map(new ZeusEventMapFunction())
                .setParallelism(parameterTool.getInt("zeus-kafka-sink-late-parallelism"))
                .name("Zeus Late Event Sink Mapper");
        if (isLocal) {
            Path path = new Path(parameterTool.get("late-output-path"));
            /*FileSink<String> fileSink = FileSink
                    .forRowFormat(path, new SimpleStringEncoder<String>())
                    .withBucketAssigner(new BasePathBucketAssigner<>())
                    .build();
            lateEventStream.sinkTo(fileSink);*/


        } else {

            //Kafka output for late events
            KafkaSink<String> lateEventProducer = ZeusKafkaSink.createKafkaSinkForLateOutput(parameterTool.get("late-output-topic"), parameterTool);
            lateEventStream.sinkTo(lateEventProducer)
                    .setParallelism(parameterTool.getInt("zeus-kafka-sink-late-parallelism"))
                    .name("Zeus Late Event Sink");
        }

        env.execute("Real Time QoS Event Consolidator");
    }

    private static void setStreamExecutionEnvironmentConfig(StreamExecutionEnvironment env) throws Exception {
        env.getConfig().setGlobalJobParameters(parameterTool);

        env.setMaxParallelism(parameterTool.getInt("max-parallelism", 155));
        env.setParallelism(parameterTool.getInt("parallelism", 31));

        env.getCheckpointConfig()
                .setCheckpointTimeout(parameterTool.getLong("checkpoint-timeout-ms", 1200000L));
        env.getCheckpointConfig()
                .setMinPauseBetweenCheckpoints(parameterTool.getLong("min-pause-between-checkpoints-ms", 5000L));
        env.getCheckpointConfig()
                .setTolerableCheckpointFailureNumber(parameterTool.getInt("tolerable-checkpoint-failure-number", 10));
        env.getCheckpointConfig()
                .setCheckpointingMode(CheckpointingMode.AT_LEAST_ONCE);

        if (parameterTool.has("checkpoint-enabled")) {
            EmbeddedRocksDBStateBackend backend = new EmbeddedRocksDBStateBackend(true);

            env.setStateBackend(backend);
            env.getCheckpointConfig().setCheckpointStorage(new URI(parameterTool.getRequired("checkpoint-location")));
            env.enableCheckpointing(parameterTool.getLong("checkpoint-interval-ms", 300000L));
            env.getCheckpointConfig()
                    .enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        }
    }


    private static ParameterTool loadLocalEnvironmentProperties() {
        Properties properties = new Properties();

        //Flink
        properties.put("parallelism", "1");
        properties.put("max-parallelism", "200");
        properties.put("events-threshold-count","50");
        properties.put("zeus-kafka-source-parallelism", "1");
        properties.put("zeus-watermark-parallelism", "1");
        properties.put("zeus-aggregator-parallelism", "1");
        properties.put("zeus-kafka-sink-parallelism", "1");
        properties.put("zeus-kafka-sink-late-parallelism", "1");


        //Checkpoint
        properties.put("checkpoint-enabled", "true");
        properties.put("checkpoint-location", "file:///Users/swathi.veeramalla/realtime-qos/checkpoints");
        properties.put("checkpoint-interval-ms", "1000");
        properties.put("checkpoint-timeout-ms", "60000");
        properties.put("min-pause-between-checkpoints-ms", "5000");
        properties.put("tolerable-checkpoint-failure-number", "10");

//        properties.put("output-path", "file:///Users/hramesh.pc/realtime-qos/output");
//        properties.put("input-path", "file:///Users/hramesh.pc/realtime-qos/input");
        final String app_path = System.getenv("APP_PATH");
        properties.put("input-path", "file:///Users/swathi.veeramalla/IdeaProjects/Flink-Project/src/main/resources/test.json");
        properties.put("output-path", "file:///Users/swathi.veeramalla/IdeaProjects/test-data/output/");
        properties.put("late-output-path", "file:///Users/hramesh.pc/realtime-qos/late-output");

        //Kafka cluster
        properties.put("bootstrap.servers", "10.155.248.244:9092");
        properties.put("output-topic", "realtime-playback");
        properties.put("late-output-topic", "realtime-late-events");

        //Kafka source
        properties.put("kafka-consumer-topic-name", "statspost-event-qos-realtime");
        properties.put("kafka-consumer-group-id", "realtime-consumer-v2");
        properties.put("kafka-consumer-startup-mode", "earliest");
        properties.put("kafka-producer-fault-tolerance-semantic", "at_least_once");
        properties.put("kafka-consumer-checkpoint-enabled", "true");
        properties.put("flink.partition-discovery.interval-millis", "5000");
        properties.put("enable.auto.commit", "true");
        properties.put("auto.commit.interval.ms", "5000");
        properties.put("auto.offset.reset", "latest");
        properties.put("group.id", "statspost-event-qos-consumer-v1");
        return fromApplicationProperties(properties);
    }


}
