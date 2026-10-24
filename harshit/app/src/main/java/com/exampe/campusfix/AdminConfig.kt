package com.exampe.campusfix

/**
 * Admin Configuration for CampusFix Hackathon Demonstration.
 *
 * To enable Admin access:
 * 1. The designated admin credentials are set below.
 * 2. In Firebase Console -> Authentication -> Users, create an account with
 *    Email: admin@campus.edu (or your preferred admin email).
 * 3. In `firestore.rules`, this email is recognized as administrator.
 */
object AdminConfig {
    const val ADMIN_EMAIL: String = "admin@campus.edu"
    const val ADMIN_PASSWORD: String = "admin123"

    /**
     * Checks if the given email corresponds to the authorized administrator.
     */
    fun isAdmin(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return email.trim().equals(ADMIN_EMAIL, ignoreCase = true)
    }
}
