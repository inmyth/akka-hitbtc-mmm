package com.mbcu.hitbtc.mmm.utils

import java.util.Date
import java.util.logging.{Level, Logger}

object MyLoggingSingle {

  import java.io.IOException
  import java.util.TimeZone
  import java.util.logging.ConsoleHandler
  import java.util.logging.FileHandler
  import java.util.logging.Handler
  import java.util.logging.SimpleFormatter

  import java.text.SimpleDateFormat

  private val LOG_NAME = "./log/log.%s.txt"
  private val limit = 1024 * 1024 * 20 // 20 Mb
  private val numLogFiles = 20
  private val sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SS")

  var log : Logger = {
    val logger = Logger.getLogger("")
    logger.setLevel(Level.ALL)
    val timeStamp = sdf.format(new Date())
    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"))
    val fileName = String.format(LOG_NAME, timeStamp)
    val fileHandler = new FileHandler(fileName, limit, numLogFiles, true)
    // Create txt Formatter
    val formatter = new SimpleFormatter
    fileHandler.setFormatter(formatter)
    logger.addHandler(fileHandler)
//    val consoleHandler = new ConsoleHandler
//    consoleHandler.setLevel(Level.ALL)
//    consoleHandler.setFormatter(formatter)
//    logger.addHandler(consoleHandler)
    logger
  }


}
