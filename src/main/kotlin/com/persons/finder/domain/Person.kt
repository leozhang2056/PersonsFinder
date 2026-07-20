package com.persons.finder.domain

import javax.persistence.*

@Entity
@Table(name = "persons")
class Person(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @Column(name = "job_title")
    var jobTitle: String = "",

    var hobbies: String = "",

    var bio: String = ""
)
