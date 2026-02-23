package com.funTrip.fun2go.data.model

data class DistanceMatrixResponse(
    val status: String,
    val rows: List<Row>?
) {
    data class Row(val elements: List<Element>?)
    data class Element(
        val status: String,
        val distance: TextValue?,
        val duration: TextValue?
    )
    data class TextValue(val text: String, val value: Int)
}
