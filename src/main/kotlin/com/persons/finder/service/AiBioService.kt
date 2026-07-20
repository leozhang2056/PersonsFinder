package com.persons.finder.service

import java.util.concurrent.CompletableFuture

interface AiBioService {
    fun generateBio(jobTitle: String, hobbies: List<String>): CompletableFuture<String>

    fun generateBio(name: String, jobTitle: String, hobbies: List<String>): CompletableFuture<String> {
        return generateBio(jobTitle, hobbies)
    }
}
