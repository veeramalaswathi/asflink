/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.Flink.Project;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.TextInputFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.FileProcessingMode;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.time.Duration;

/**
 * Skeleton for a Flink Batch Job.
 *
 * <p>For a tutorial how to write a Flink batch application, check the
 * tutorials and examples on the <a href="http://flink.apache.org/docs/stable/">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution,
 * change the main class in the POM.xml file to this class (simply search for 'mainClass')
 * and run 'mvn clean package' on the command line.
 */
public class BatchJob {

	private transient ValueState<ZeusPlayback> appCrashedState;
	private transient ValueState<ZeusPlayback> appStartState;

	public static void main(String[] args) throws Exception {
		// set up the batch execution environment
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();


		//env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

		// connect to the data file
		//DataSet<String> carData = env.readTextFile("/Users/swathi.veeramalla/IdeaProjects/Flink-Project/src/main/resources/test.json");

		String path = "/Users/swathi.veeramalla/IdeaProjects/Flink-Project/src/main/resources/test.json";
		/*TextInputFormat auditFormat = new TextInputFormat(
				new Path(path));

		//Create a Datastream based on the directory
		DataStream<String> auditTrailStr
				= env.readFile(auditFormat,
				path,    //Director to monitor
				FileProcessingMode.PROCESS_CONTINUOUSLY,
				1000);

		auditTrailStr.print();*/
				//.assignTimestampsAndWatermarks(new ConnectedCarAssigner());
		/*DataStream<ZeusEvent> parsedData = auditTrailStr
				.map(new ZeusRawEventsMapper())
				.filter(new ZeusFilterFunction(50))
				.setParallelism(1)
				.name("Zeus Kafka Source");*/

		DataStreamSource<String> rawInput = env.readFile(
				new TextInputFormat(new Path(path)), path);
		//rawInput.print();

		DataStream<ZeusEvent> data = rawInput.map(new ZeusRawEventsMapper())
				.filter(new ZeusFilterFunction(50))
				.setParallelism(1)
				.name("Zeus Kafka Source");
		DataStream<ZeusEvent> appCrashed = data.filter(new FilterFunction<ZeusEvent>() {
			@Override
			public boolean filter(ZeusEvent zeusEvent) throws Exception {
				if(zeusEvent.getEventTypeName().equalsIgnoreCase("videoheartbeat")){
					return true;
				}else
					return false;
			}
		});

		DataStream<ZeusEvent> appStart = data.filter(new FilterFunction<ZeusEvent>() {
			@Override
			public boolean filter(ZeusEvent zeusEvent) throws Exception {
				if(zeusEvent.getEventTypeName().equalsIgnoreCase("SeekEnded")){
					return true;
				}else
					return false;
			}
		});



		/*events.keyBy("carId")
				.window(EventTimeSessionWindows.withGap(Time.seconds(15)))
				.apply(new CreateGapSegment())
				.print();*/

		env.execute("Driving Sessions");
	}


}
