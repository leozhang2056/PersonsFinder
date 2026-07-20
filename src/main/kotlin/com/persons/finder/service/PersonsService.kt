package com.persons.finder.service

import com.persons.finder.domain.Person

interface PersonsService {
    fun getById(id: Long): Person
    fun save(person: Person): Person
    fun saveAll(persons: List<Person>): List<Person>
    fun findAll(): List<Person>
    fun generateSeedBio(jobTitle: String, hobbies: List<String>): String
}
