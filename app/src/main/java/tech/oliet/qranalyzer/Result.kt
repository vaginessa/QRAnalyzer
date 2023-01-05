package tech.oliet.qranalyzer

data class Result(val contents: String, val errorCorrectionLevel: String, val rawBytes: ByteArray) :
    java.io.Serializable