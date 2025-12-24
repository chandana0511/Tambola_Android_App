package com.example.tambola

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PlayerViewModel : ViewModel() {

    private val database: DatabaseReference = FirebaseDatabase.getInstance("https://tambola-app-2823c-default-rtdb.asia-southeast1.firebasedatabase.app").reference
    private var roomCode: String? = null
    private var playerId: String? = null

    // LiveData for UI
    val ticket = MutableLiveData<Array<IntArray>?>()
    val markedNumbers = MutableLiveData<Set<Int>>()
    val calledNumbers = MutableLiveData<Set<Int>>()
    val winners = MutableLiveData<Map<String, String>>()
    val gameStatus = MutableLiveData<String>()
    val resetVersion = MutableLiveData<Long>()
    val error = MutableLiveData<String>()

    private val ticketListener: ValueEventListener = createValueEventListener { snapshot ->
        val ticketData = snapshot.value as? List<List<Long>>
        ticket.value = ticketData?.map { row -> row.map { it.toInt() }.toIntArray() }?.toTypedArray()
    }

    private val markedNumbersListener: ValueEventListener = createValueEventListener { snapshot ->
        markedNumbers.value = snapshot.children.mapNotNull { it.getValue(Int::class.java) }.toSet()
    }

    private val calledNumbersListener: ValueEventListener = createValueEventListener { snapshot ->
        calledNumbers.value = snapshot.children.mapNotNull { it.getValue(Int::class.java) }.toSet()
    }

    private val claimsListener: ValueEventListener = createValueEventListener { snapshot ->
        val winnersMap = mutableMapOf<String, String>()
        snapshot.children.forEach { claimSnapshot ->
            val claimType = claimSnapshot.key!!
            val winnerName = claimSnapshot.value.toString()
            winnersMap[claimType] = winnerName
        }
        winners.value = winnersMap
    }

    private val statusListener: ValueEventListener = createValueEventListener { snapshot ->
        gameStatus.value = snapshot.getValue(String::class.java)
    }

    private val resetVersionListener: ValueEventListener = createValueEventListener { snapshot ->
        resetVersion.value = snapshot.getValue(Long::class.java) ?: 1L
    }

    fun init(roomCode: String, playerId: String) {
        this.roomCode = roomCode
        this.playerId = playerId
        attachListeners()
    }

    private fun attachListeners() {
        roomCode?.let {
            val roomRef = database.child("rooms").child(it)
            playerId?.let { pid ->
                roomRef.child("tickets").child(pid).addValueEventListener(ticketListener)
                roomRef.child("markedNumbers").child(pid).addValueEventListener(markedNumbersListener)
            }
            roomRef.child("calledNumbers").addValueEventListener(calledNumbersListener)
            roomRef.child("claims").addValueEventListener(claimsListener)
            roomRef.child("status").addValueEventListener(statusListener)
            roomRef.child("resetVersion").addValueEventListener(resetVersionListener)
        }
    }

    override fun onCleared() {
        super.onCleared()
        roomCode?.let {
            val roomRef = database.child("rooms").child(it)
            playerId?.let { pid ->
                roomRef.child("tickets").child(pid).removeEventListener(ticketListener)
                roomRef.child("markedNumbers").child(pid).removeEventListener(markedNumbersListener)
            }
            roomRef.child("calledNumbers").removeEventListener(calledNumbersListener)
            roomRef.child("claims").removeEventListener(claimsListener)
            roomRef.child("status").removeEventListener(statusListener)
            roomRef.child("resetVersion").removeEventListener(resetVersionListener)
        }
    }

    private fun createValueEventListener(onDataChange: (DataSnapshot) -> Unit): ValueEventListener {
        return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) = onDataChange(snapshot)
            override fun onCancelled(databaseError: DatabaseError) {
                error.value = databaseError.message
            }
        }
    }
     fun markNumber(number: Int) {
        roomCode?.let { code ->
            playerId?.let { pId ->
                val currentMarked = markedNumbers.value?.toMutableSet() ?: mutableSetOf()
                if (currentMarked.add(number)) {
                    database.child("rooms").child(code).child("markedNumbers").child(pId).setValue(currentMarked.toList())
                }
            }
        }
    }
}
