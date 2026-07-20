package com.persons.finder.vo

data class PersonResponse(
    val id: Long,
    val name: String,
    val jobTitle: String,
    val hobbies: List<String>,
    val bio: String,
    val location: LocationResponse? = null
)
