package com.xmlweave

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

@SpringBootApplication
open class XmlWeaveApplication : SpringBootServletInitializer()

fun main(args: Array<String>) {
    runApplication<XmlWeaveApplication>(*args)
}