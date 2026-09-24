package com.example.egimhesabi.data

object ElementStatus {
    const val PLANNED = "PLANNED"
    const val IN_PRODUCTION = "IN_PRODUCTION"
    const val NOT_STARTED = "NOT_STARTED"
    const val COMPLETED = "COMPLETED"
    const val PROGRESS_PAYMENT = "PROGRESS_PAYMENT"
    const val CANCELLED = "CANCELLED"

    val pipelineStatuses = setOf(PLANNED, IN_PRODUCTION, PROGRESS_PAYMENT)
    val manholeStatuses = setOf(NOT_STARTED, COMPLETED, PROGRESS_PAYMENT, CANCELLED)
}
