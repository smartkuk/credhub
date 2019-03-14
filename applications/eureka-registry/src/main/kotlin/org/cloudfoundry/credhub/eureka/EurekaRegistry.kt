package org.cloudfoundry.credhub.eureka

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer

@EnableEurekaServer
@SpringBootApplication
class EurekaRegistry

fun main(args: Array<String>) {
    SpringApplication.run(EurekaRegistry::class.java, *args)
    println("Hello, world; I'm a Eureka Registry")
}
