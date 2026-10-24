package com.exampe.campusfix

/**
 * Data model for a Campus Complaint stored in Firestore.
 * Default values are provided to support Firebase automatic deserialization.
 */
data class Complaint(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val category: String = "",
    val location: String = "",
    val description: String = "",
    val status: String = "Pending",
    val timestamp: Long = System.currentTimeMillis()
)

object ComplaintCategories {
    const val CLASSROOM = "Classroom"
    const val ELECTRICITY = "Electricity"
    const val WATER = "Water"
    const val CLEANLINESS = "Cleanliness"
    const val OTHER = "Other"

    val list = listOf(
        CLASSROOM,
        ELECTRICITY,
        WATER,
        CLEANLINESS,
        OTHER
    )
}

object ComplaintStatuses {
    const val PENDING = "Pending"
    const val IN_PROGRESS = "In Progress"
    const val RESOLVED = "Resolved"

    val all = listOf(PENDING, IN_PROGRESS, RESOLVED)
}

data class ComplaintSummary(
    val total: Int = 0,
    val pending: Int = 0,
    val inProgress: Int = 0,
    val resolved: Int = 0
)
