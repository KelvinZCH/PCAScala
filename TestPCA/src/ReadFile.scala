/**
  * Created by root on 3/6/17.
  */

import org.apache.spark.mllib.feature.PCA
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.rdd.RDD
import breeze.linalg._
import breeze.numerics._
import com.clearspring.analytics.util.Varint
import org.apache.hadoop.fs.FileSystem.Statistics
import org.apache.hadoop.hive.ql.exec.vector.mapjoin.fast.VectorMapJoinFastLongHashUtil
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.linalg.Matrix
import scala.util.control._
import org.apache.spark.{SparkConf, SparkContext}
import shapeless.Nat._0


object ReadFromObject {

  def PCAWithGateWay(RawMat:RowMatrix,percent:Int): (Matrix,Vector) =
  {
    var loop = new Breaks;


    val resultMat = RawMat.computePrincipalComponentsAndExplainedVariance(2114)
    val VarianceArr = resultMat._2

    val ArrValue =  VarianceArr.toArray

    val sum = ArrValue.sum
    var current:Double = 0
    var count:Int = 0

      loop.breakable(

        for(s<-ArrValue)
        {
          current+=s
          val percent = (current/sum *100).toInt

          //println("CurrentID: ",count,"Sum Current: ",current,"Total: ", sum,"Percent: ",percent)

          if(current > percent)
            loop.break

          count += 1

        }
      )

      return RawMat.computePrincipalComponentsAndExplainedVariance(count)



  }

  def main(arg: Array[String]): Unit = {

    val SequenialFilePath = "hdfs://S1:9000/SequentialFile/001/"

    val conf = new SparkConf().setAppName("ReadObject").setMaster("local")
    val sc = new SparkContext(conf)

    sc.setLogLevel("WARN")

    val MatrixRDD:RDD[Array[Double]] = sc.objectFile[Array[Double]](SequenialFilePath).filter(f=>f.size==2114)

    val MatrixVector = MatrixRDD.map(f=>Vectors.dense(f))

    MatrixVector.take(10).foreach(println)

    val RawMatrix = new RowMatrix(MatrixVector)

    //RawMatrix.computePrincipalComponents(20)
    val Sum = RawMatrix.computeColumnSummaryStatistics()
    println(Sum.max)
    println(Sum.min)
    println(Sum.mean)

    val resultSet = PCAWithGateWay(RawMatrix,80)
    val ResArrFilter = resultSet._2.toArray

    for(s<-ResArrFilter)
    {
      println(s)
    }
/*
    val resultMat = RawMatrix.computePrincipalComponentsAndExplainedVariance(2114)
    val VarianceArr = resultMat._2

     val ArrValue =  VarianceArr.toArray

     val sum = ArrValue.sum
     var current:Double = 0
    var count:Int = 0
    for(s<-ArrValue)
    {
      current+=s
      val percent = (current/sum *100).toInt

      println("CurrentID: ",count,"Sum Current: ",current,"Total: ", sum,"Percent: ",percent)
      count += 1

    }

*/

  }

}
