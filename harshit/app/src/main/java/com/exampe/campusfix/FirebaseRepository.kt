package com.exampe.campusfix

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Clean, lightweight repository handling Firebase Authentication and Firestore operations.
 * Safely guards against missing Firebase configuration so the app won't crash on startup.
 */
class FirebaseRepository(private val context: Context) {

    val isConfigured: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Throwable) {
            false
        }

    private val auth: FirebaseAuth?
        get() = try {
            if (isConfigured) FirebaseAuth.getInstance() else null
        } catch (_: Throwable) {
            null
        }

    private val firestore: FirebaseFirestore?
        get() = try {
            if (isConfigured) FirebaseFirestore.getInstance() else null
        } catch (_: Throwable) {
            null
        }

    fun getCurrentUser(): FirebaseUser? = auth?.currentUser

    // -------------------------------------------------------------------------
    // AUTHENTICATION
    // -------------------------------------------------------------------------

    fun login(
        email: String,
        pass: String,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        val authClient = auth ?: run {
            onResult(Result.failure(Exception("Firebase is not initialized. Please place google-services.json in the 'app' folder.")))
            return
        }

        authClient.signInWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                result.user?.let { onResult(Result.success(it)) }
                    ?: onResult(Result.failure(Exception("Unable to get signed-in user.")))
            }
            .addOnFailureListener { err ->
                onResult(Result.failure(err))
            }
    }

    fun register(
        email: String,
        pass: String,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        val authClient = auth ?: run {
            onResult(Result.failure(Exception("Firebase is not initialized. Please place google-services.json in the 'app' folder.")))
            return
        }

        authClient.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                result.user?.let { onResult(Result.success(it)) }
                    ?: onResult(Result.failure(Exception("Registration succeeded, but user info was missing.")))
            }
            .addOnFailureListener { err ->
                onResult(Result.failure(err))
            }
    }

    fun logout() {
        auth?.signOut()
    }

    // -------------------------------------------------------------------------
    // FIRESTORE COMPLAINT OPERATIONS
    // -------------------------------------------------------------------------

    /**
     * Submits a new complaint for the currently authenticated student.
     * Default status is always set to 'Pending'.
     */
    fun submitComplaint(
        category: String,
        location: String,
        description: String,
        onResult: (Result<Complaint>) -> Unit
    ) {
        val user = auth?.currentUser ?: run {
            onResult(Result.failure(Exception("You must be logged in to file a complaint.")))
            return
        }

        val db = firestore ?: run {
            onResult(Result.failure(Exception("Firestore is not available.")))
            return
        }

        val docRef = db.collection("complaints").document()
        val complaint = Complaint(
            id = docRef.id,
            userId = user.uid,
            userEmail = user.email ?: "student@campus.edu",
            category = category.trim(),
            location = location.trim(),
            description = description.trim(),
            status = ComplaintStatuses.PENDING,
            timestamp = System.currentTimeMillis()
        )

        docRef.set(complaint)
            .addOnSuccessListener {
                onResult(Result.success(complaint))
            }
            .addOnFailureListener { err ->
                onResult(Result.failure(err))
            }
    }

    /**
     * Fetches only complaints belonging to the logged-in student.
     */
    fun getMyComplaints(onResult: (Result<List<Complaint>>) -> Unit) {
        val user = auth?.currentUser ?: run {
            onResult(Result.failure(Exception("You must be logged in to view your complaints.")))
            return
        }

        val db = firestore ?: run {
            onResult(Result.failure(Exception("Firestore is not available.")))
            return
        }

        db.collection("complaints")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { it.toObject(Complaint::class.java) }
                    .sortedByDescending { it.timestamp }
                onResult(Result.success(list))
            }
            .addOnFailureListener { err ->
                onResult(Result.failure(err))
            }
    }

    /**
     * Fetches all complaints across all students (Admin operation).
     */
    fun getAllComplaints(onResult: (Result<List<Complaint>>) -> Unit) {
        val db = firestore ?: run {
            onResult(Result.failure(Exception("Firestore is not available.")))
            return
        }

        db.collection("complaints")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { it.toObject(Complaint::class.java) }
                    .sortedByDescending { it.timestamp }
                onResult(Result.success(list))
            }
            .addOnFailureListener { err ->
                onResult(Result.failure(err))
            }
    }

    /**
     * Updates the status of a complaint (Admin operation: Pending / In Progress / Resolved).
     */
    fun updateComplaintStatus(
        complaintId: String,
        newStatus: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val db = firestore ?: run {
            onResult(Result.failure(Exception("Firestore is not available.")))
            return
        }

        db.collection("complaints").document(complaintId)
            .update("status", newStatus)
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { err ->
                onResult(Result.failure(err))
            }
    }
}
