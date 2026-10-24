package com.doron.gymtracker.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doron.gymtracker.data.db.dao.SessionDao
import com.doron.gymtracker.data.db.entities.SessionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDaoTest {

    private lateinit var db: GymDatabase
    private lateinit var dao: SessionDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.sessionDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertAndFinishSession() = runTest {
        val id = dao.insert(SessionEntity(0, "workout_a", startedAt = 1000L, finishedAt = null))
        assertNull(dao.getLastCompleted())

        dao.update(SessionEntity(id, "workout_a", 1000L, finishedAt = 2000L))
        val last = dao.getLastCompleted()
        assertNotNull(last)
        assertEquals(id, last!!.id)
    }
}
