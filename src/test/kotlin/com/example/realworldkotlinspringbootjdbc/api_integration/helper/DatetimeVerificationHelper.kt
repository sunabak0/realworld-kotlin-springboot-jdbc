package com.example.realworldkotlinspringbootjdbc.api_integration.helper

import org.assertj.core.api.Assertions.assertThat
import java.time.OffsetDateTime

object DatetimeVerificationHelper {
    fun expectIso8601UtcAndParsable(o: Any): Boolean {
        assertThat(o.toString())
            .`as`("YYYY-MM-DDTHH:mm:ss.SSSXXX(ISO8601形式)で、TimeZoneはUTCである")
            .matches("([0-9]{4})-([0-9]{2})-([0-9]{2}T([0-9]{2}):([0-9]{2}):([0-9]{2}).([0-9]{3})Z)")
        return runCatching { OffsetDateTime.parse(o.toString()) }.isSuccess
    }

    fun expectIso8601UtcWithoutMillisecondAndParsable(o: Any): Boolean {
        assertThat(o.toString())
            .`as`("YYYY-MM-DDTHH:mm:ss.SSSXXX(ISO8601形式)で、TimeZoneはUTCである")
            .matches("([0-9]{4})-([0-9]{2})-([0-9]{2}T([0-9]{2}):([0-9]{2}):([0-9]{2})Z)")
        return runCatching { OffsetDateTime.parse(o.toString()) }.isSuccess
    }
}
