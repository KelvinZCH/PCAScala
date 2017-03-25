/**
  * Created by root on 2/26/17.
  */

import java.io.PrintStream
//import java.nio.file.FileSystem

import org.apache.spark.mllib.feature.PCA
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.hadoop.fs.FileSystem
import breeze.linalg._
import breeze.numerics._
import org.apache.hadoop.hive.ql.exec.vector.mapjoin.fast.VectorMapJoinFastLongHashUtil
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable.ArrayBuffer

object PCAObject {

  def fileterVoltage(InputStr:String):Float =
  {
    val strlist = InputStr.split('\t')
    val StrVotage:String = "Voltage"
    val StrValue:String = strlist(1)

    val ret = StrValue.compareToIgnoreCase(StrVotage)

    if (ret == 1)
      return StrValue.toFloat

    return 0
  }
  def main(arg: Array[String]): Unit = {

    //Initialize the context
    val conf = new SparkConf().setAppName("PCATest").setMaster("yarn-client")
    val sc = new SparkContext(conf)
    val HdfsPath = "hdfs://Master.Hadoop:9000/ECG/Interpolation/*MLII_2114*"
    val FormatString:String = "hdfs://Master.Hadoop:9000/ECG/Interpolation/%S_MLII_2114.csv"
    val SelectedDataPath = "hdfs://Master.Hadoop:9000/ECG/SelectedData/SelectedData.txt"
    val SequenialFilePath = "hdfs://Master.Hadoop:9000/SequentialFile/001/"
    sc.setLogLevel("WARN")

    val accu = sc.longAccumulator("RowCounter")
    //Start Selected Data file
    println("Start Read Selected Text File")
    val listSelectFile = sc.textFile(SelectedDataPath).map(x=>x.split('\t')).map(x=>x(0)).collect().tail

    var counter:Int = 0
    var RDDInGen :RDD[Array[Double]] = null

    for(item<-listSelectFile)
    {
        val strFormatSting = String.format(FormatString,item)
         //println(strFormatSting)

          //Process txt file
        val RawFile = sc.textFile(strFormatSting)
        //RawFile.take(10).foreach(println)


        val RawValues = RawFile.map(x=>x.split(','))
          .filter((s)=>(!s(1).contains("Voltage")))
          .map(x=>x(1).toDouble)

        //val VectorVal = Vectors.dense(RawValues

        //RawValues.take(10).foreach(println)

        val IndexWithVoltage = RawValues.zipWithIndex().mapValues(s=>s/2114).groupBy(_._2).sortByKey()
        println(counter)
        println(strFormatSting)
        println(RawValues.count()/2114)
        IndexWithVoltage.take(1).foreach(println)
/*
*           val ArrBuf = ArrayBuffer[Double]()
          for(itemit<-s)
          {
            ArrBuf += itemit._1
          }
          ArrBuf.toArray*/
        val Voltagein2114 = IndexWithVoltage.mapValues(s=>
        {
          val ArrBuf = ArrayBuffer[Double]()
          for(itemit<-s)
          {
            ArrBuf += itemit._1
          }

          val VoltageArr = ArrBuf.toArray
          VoltageArr
        }).values

        if(counter == 0)
        {
          RDDInGen =Voltagein2114
        }
        else
        {
          RDDInGen = RDDInGen.union(Voltagein2114)
        }

        counter +=1

        /*Voltagein2114.take(10).foreach(println)

        val Voltagein2114Vectors = Voltagein2114.map(f => Vectors.dense(f))

        val RawMatrix = new RowMatrix(Voltagein2114Vectors)

        //RawMatrix.computePrincipalComponents(20)
        val Sum = RawMatrix.computeColumnSummaryStatistics()
        println(Sum.max)
        println(Sum.min)
        println(Sum.mean)*/

    }
    val hdfs = FileSystem.get(new java.net.URI("hdfs://Master.Hadoop:9000"),new org.apache.hadoop.conf.Configuration())
    val path = new org.apache.hadoop.fs.Path(SequenialFilePath)

    if(hdfs.exists(path))
    {
        hdfs.delete(path,false)
    }
    RDDInGen.saveAsObjectFile(SequenialFilePath)

    // Compose the file path

  }

}
