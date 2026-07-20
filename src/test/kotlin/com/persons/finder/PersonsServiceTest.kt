package com.persons.finder

import com.persons.finder.domain.Person
import com.persons.finder.mapper.PersonRepository
import com.persons.finder.service.PersonsServiceImpl
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Optional

@ExtendWith(MockKExtension::class)
class PersonsServiceTest {

    @MockK
    private lateinit var personRepository: PersonRepository

    private lateinit var personsService: PersonsServiceImpl

    @BeforeEach
    fun setUp() {
        personsService = PersonsServiceImpl(personRepository)
    }

    @Test
    fun `save should persist person`() {
        val person = Person(name = "John", jobTitle = "Engineer")
        every { personRepository.save(person) } returns person

        val result = personsService.save(person)

        assertEquals(person, result)
        verify { personRepository.save(person) }
    }

    @Test
    fun `getById should return person when exists`() {
        val person = Person(id = 1, name = "John", jobTitle = "Engineer")
        every { personRepository.findById(1L) } returns Optional.of(person)

        val result = personsService.getById(1)

        assertEquals("John", result.name)
        assertEquals("Engineer", result.jobTitle)
    }

    @Test
    fun `getById should throw NoSuchElementException when not found`() {
        every { personRepository.findById(999L) } returns Optional.empty()

        assertThrows(NoSuchElementException::class.java) {
            personsService.getById(999)
        }
    }

    @Test
    fun `findAll should return all persons`() {
        val persons = listOf(
            Person(id = 1, name = "Alice"),
            Person(id = 2, name = "Bob")
        )
        every { personRepository.findAll() } returns persons

        val result = personsService.findAll()

        assertEquals(2, result.size)
        assertEquals("Alice", result[0].name)
    }

    @Test
    fun `findAll should return empty list when no persons`() {
        every { personRepository.findAll() } returns emptyList()

        val result = personsService.findAll()

        assertEquals(0, result.size)
    }

    @Test
    fun `existsById should return true when person exists`() {
        every { personRepository.existsById(1L) } returns true

        val result = personsService.existsById(1)

        assertEquals(true, result)
    }

    @Test
    fun `existsById should return false when person does not exist`() {
        every { personRepository.existsById(999L) } returns false

        val result = personsService.existsById(999)

        assertEquals(false, result)
    }

    @Test
    fun `saveAll should persist multiple persons`() {
        val persons = listOf(
            Person(name = "Alice", jobTitle = "Engineer"),
            Person(name = "Bob", jobTitle = "Designer")
        )
        every { personRepository.saveAll(persons) } returns persons

        val result = personsService.saveAll(persons)

        assertEquals(2, result.size)
        verify { personRepository.saveAll(persons) }
    }

    @Test
    fun `generateSeedBio should return expected template`() {
        val bio = personsService.generateSeedBio("Engineer", listOf("hiking", "chess"))

        assertEquals("A Engineer who turns hiking and chess into surprisingly good conversation.", bio)
    }
}
