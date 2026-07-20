package com.persons.finder

import com.persons.finder.domain.Person
import com.persons.finder.mapper.PersonRepository
import com.persons.finder.service.PersonsServiceImpl
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class PersonsServiceTest {

    private lateinit var personRepository: PersonRepository
    private lateinit var personsService: PersonsServiceImpl

    @BeforeEach
    fun setUp() {
        personRepository = io.mockk.mockk()
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
    fun `findAllPaginated should return paginated results`() {
        val persons = listOf(
            Person(id = 11, name = "Alice_11"),
            Person(id = 12, name = "Bob_12")
        )
        every { personRepository.findAllWithLimit(10L, 10L) } returns persons

        val result = personsService.findAllPaginated(1, 10)

        assertEquals(2, result.size)
        assertEquals("Alice_11", result[0].name)
    }

    @Test
    fun `findAllPaginated page 0 should start from offset 0`() {
        every { personRepository.findAllWithLimit(20L, 0L) } returns emptyList()

        val result = personsService.findAllPaginated(0, 20)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `countAll should return total count`() {
        every { personRepository.countAll() } returns 1000000L

        val result = personsService.countAll()

        assertEquals(1000000L, result)
    }

    @Test
    fun `countAll should return 0 when empty`() {
        every { personRepository.countAll() } returns 0L

        assertEquals(0L, personsService.countAll())
    }

    @Test
    fun `existsById should return true when person exists`() {
        every { personRepository.existsById(1L) } returns true

        assertTrue(personsService.existsById(1))
    }

    @Test
    fun `existsById should return false when person does not exist`() {
        every { personRepository.existsById(999L) } returns false

        assertFalse(personsService.existsById(999))
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

    @Test
    fun `generateSeedBio should use default when job is blank`() {
        val bio = personsService.generateSeedBio("", listOf("reading"))

        assertTrue(bio.contains("curious human"))
    }
}
