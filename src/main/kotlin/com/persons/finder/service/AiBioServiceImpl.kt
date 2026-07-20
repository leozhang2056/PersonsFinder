package com.persons.finder.service

import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class AiBioServiceImpl : AiBioService {

    // ==================== Bio generation ====================

    override fun generateBio(jobTitle: String, hobbies: List<String>): CompletableFuture<String> {
        val safeJob = sanitize(jobTitle).ifBlank { "curious human" }
        val safeHobbies = hobbies.map(::sanitize).filter { it.isNotBlank() }.take(5)
        val hobbyText = when (safeHobbies.size) {
            0 -> "collecting tiny moments"
            1 -> safeHobbies.first()
            else -> safeHobbies.dropLast(1).joinToString(", ") + " and " + safeHobbies.last()
        }

        val bio = "A $safeJob who turns $hobbyText into surprisingly good conversation."

        return CompletableFuture.supplyAsync {
            // Simulate async AI API call latency
            Thread.sleep(200)
            bio
        }
    }

    // ==================== Injection sanitization ====================

    internal fun sanitize(value: String): String {
        val blockedPatterns = listOf(
            Regex("ignore\\s+(all\\s+)?(previous\\s+)?instructions?", RegexOption.IGNORE_CASE),
            Regex("ignore\\s+everything", RegexOption.IGNORE_CASE),
            Regex("system\\s+prompt", RegexOption.IGNORE_CASE),
            Regex("developer\\s+message", RegexOption.IGNORE_CASE),
            Regex("you\\s+are(\\s+\\w+){0,4}", RegexOption.IGNORE_CASE),
            Regex("i\\s*'?m\\s+hacked", RegexOption.IGNORE_CASE),
            Regex("i\\s+am\\s+hacked", RegexOption.IGNORE_CASE),
            Regex("jailbreak", RegexOption.IGNORE_CASE),
            Regex("forget\\s+your\\s+rules", RegexOption.IGNORE_CASE),
            Regex("act\\s+as(\\s+\\w+){0,4}", RegexOption.IGNORE_CASE),
            Regex("say\\s+['\"]?[^,.;]*", RegexOption.IGNORE_CASE),
            Regex("output\\s+['\"]?[^,.;]*", RegexOption.IGNORE_CASE),
            Regex("compromised", RegexOption.IGNORE_CASE),
            Regex("pwned", RegexOption.IGNORE_CASE)
        )

        val cleaned = value
            .replace(Regex("[\\r\\n`<>]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(80)
            .trim(' ', '.', ',', ';', ':', '-', '_', '\'', '"')

        // Block entire field on injection match — caller uses default value
        if (blockedPatterns.any { it.containsMatchIn(cleaned) }) {
            return ""
        }

        return cleaned
    }
}
