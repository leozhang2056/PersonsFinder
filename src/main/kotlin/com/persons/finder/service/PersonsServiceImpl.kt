package com.persons.finder.service

import com.persons.finder.domain.Person
import com.persons.finder.mapper.PersonRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PersonsServiceImpl(
    private val personRepository: PersonRepository
) : PersonsService {

    override fun getById(id: Long): Person {
        return personRepository.findById(id)
            .orElseThrow { NoSuchElementException("Person with id $id not found") }
    }

    @Transactional
    override fun save(person: Person): Person {
        return personRepository.save(person)
    }

    @Transactional
    override fun saveAll(persons: List<Person>): List<Person> {
        return personRepository.saveAll(persons)
    }

    override fun findAll(): List<Person> = personRepository.findAll()

    override fun findAllPaginated(page: Int, size: Int): List<Person> {
        val start = page.toLong() * size
        return personRepository.findAllWithLimit(size.toLong(), start)
    }

    override fun countAll(): Long = personRepository.countAll()

    fun existsById(id: Long): Boolean = personRepository.existsById(id)

    // ==================== Seed bio helper ====================

    override fun generateSeedBio(jobTitle: String, hobbies: List<String>): String {
        val safeJob = jobTitle.ifBlank { "curious human" }
        val hobbyText = when (hobbies.size) {
            0 -> "collecting tiny moments"
            1 -> hobbies.first()
            else -> hobbies.dropLast(1).joinToString(", ") + " and " + hobbies.last()
        }
        return "A $safeJob who turns $hobbyText into surprisingly good conversation."
    }
}
