package org.datavec.transform.analysis

import org.apache.spark.SparkConf
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.api.java.JavaSparkContext
import org.datavec.api.records.reader.RecordReader
import org.datavec.api.records.reader.impl.csv.CSVRecordReader
import org.datavec.api.transform.DataAction
import org.datavec.api.transform.analysis.DataAnalysis
import org.datavec.api.transform.analysis.columns.DoubleAnalysis
import org.datavec.api.transform.schema.Schema
import org.datavec.api.transform.ui.HtmlAnalysis
import org.datavec.api.util.ClassPathResource
import org.datavec.api.writable.Writable
import org.datavec.spark.transform.AnalyzeSpark
import org.datavec.spark.transform.misc.StringToWritablesFunction
import org.datavec.spark.transform.utils.SparkUtils

import java.io.File
import java.util.List

/**
 * Conduct and export some basic analysis on the Iris dataset, as a stand-alone .html file.
 *
 * This functionality is still fairly basic, but can still be useful for analysis and debugging.
 *
 * @author Alex Black
 */
object IrisAnalysis {

    @throws[Exception]
    def main(args: Array[String]) {

        val schema = new Schema.Builder()
            .addColumnsDouble("Sepal length", "Sepal width", "Petal length", "Petal width")
            .addColumnInteger("Species")
            .build()

        val conf = new SparkConf()
        conf.setMaster("local[*]")
        conf.setAppName("DataVec Example")

        val sc: JavaSparkContext = new JavaSparkContext(conf)

        val directory = new ClassPathResource("IrisData/iris.txt").getFile().getParent //Normally just define your directory like "file:/..." or "hdfs:/..."
        val stringData: JavaRDD[String]  = sc.textFile(directory)

        //We first need to parse this comma-delimited (CSV) format; we can do this using CSVRecordReader:
        val rr: RecordReader = new CSVRecordReader()
        val parsedInputData: JavaRDD[java.util.List[Writable]] = stringData.map(new StringToWritablesFunction(rr))

        val maxHistogramBuckets = 10
        val dataAnalysis: DataAnalysis = AnalyzeSpark.analyze(schema, parsedInputData, maxHistogramBuckets)

        println(dataAnalysis)

        //We can get statistics on a per-column basis:
        val da: DoubleAnalysis = dataAnalysis.getColumnAnalysis("Sepal length").asInstanceOf[DoubleAnalysis]
        val minValue = da.getMin
        val maxValue = da.getMax
        val mean = da.getMean

        HtmlAnalysis.createHtmlAnalysisFile(dataAnalysis, new File("DataVecIrisAnalysis.html"))

        //To write to HDFS instead:
        //String htmlAnalysisFileContents = HtmlAnalysis.createHtmlAnalysisString(dataAnalysis)
        //SparkUtils.writeStringToFile("hdfs://your/hdfs/path/here",htmlAnalysisFileContents,sc)
    }


}
