package com.persons.finder.mapper

import com.persons.finder.domain.Person
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PersonRepository : JpaRepository<Person, Long> {

    @Query(value = "SELECT * FROM persons ORDER BY id LIMIT :limit OFFSET :offset", nativeQuery = true)
    fun findAllWithLimit(@Param("limit") limit: Long, @Param("offset") offset: Long): List<Person>

    @Query(value = "SELECT COUNT(*) FROM persons", nativeQuery = true)
    fun countAll(): Long
}
