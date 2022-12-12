package com.smith.timer

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    lateinit var projectSelector : Spinner
    lateinit var projectTitle : TextView
    lateinit var displayTime : Chronometer
    lateinit var newProjectTitle : EditText

    lateinit var projectPrefs : SharedPreferences
    lateinit var projectEditor : SharedPreferences.Editor

    private var activeProject : String? = null

    var elapsedTime : Long = 0

    var running = false

    override fun onDestroy() {
        super.onDestroy()

        saveActiveProject()
    }

    override fun onPause() {
        super.onPause()

        saveActiveProject()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectSelector = findViewById(R.id.sSelector)

        projectTitle = findViewById(R.id.tvActiveProject)
        displayTime = findViewById(R.id.cmTimeSpent)

        newProjectTitle = findViewById(R.id.etTitleInput)

        val buttonStart = findViewById<Button>(R.id.bStart)
        val buttonStop = findViewById<Button>(R.id.bStop)

        val buttonCreate = findViewById<Button>(R.id.bCreate)


        projectPrefs = getSharedPreferences("projects", Context.MODE_PRIVATE)
        projectEditor = projectPrefs.edit()

        // Used for testing
//        projectEditor.clear()
//        projectEditor.commit()

        updateProjectOptions()

        val project = getProjects().keys.elementAtOrElse(0, {null})
        if (project != null) setActiveProject(project)


        projectTitle.text = if (activeProject != null) activeProject else "Create project below"

        projectSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                setActiveProject(projectSelector.selectedItem as String)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                println("bye")
            }
        }

        buttonStart.setOnClickListener { startTimer() }
        buttonStop.setOnClickListener { stopTimer() }
        buttonCreate.setOnClickListener { createProject() }


    }

    private fun startTimer() {
        if (!running && activeProject != null) {
            updateDisplayTime()
            displayTime.start()
            running = true
        }
    }

    private fun stopTimer() {
        if (running) {
            val stopTime = SystemClock.elapsedRealtime()
            displayTime.stop()
            val startTime = displayTime.getBase()
            elapsedTime = stopTime - startTime
            running = false
        }
    }

    private fun createProject() {
        val project = newProjectTitle.text.toString()
        if (project == "") return
        stopTimer()

        projectEditor.putLong(project, 0)
        projectEditor.commit()

        updateProjectOptions()

        // Spinner should select this project now
        projectSelector.setSelection(getProjects().keys.toList().indexOf(project))

        newProjectTitle.text.clear()
    }

    private fun updateProjectOptions() {
        val projects = getProjects()

        // Populate projectSelector
        ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            projects.keys.toMutableList()
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            projectSelector.adapter = adapter
        }
    }

    private fun setActiveProject(project : String?) {
        // Save the current project to preference file
        saveActiveProject()

        // Switch to project
        activeProject = project
        elapsedTime = getProjects().get(activeProject)!!

        // Update display
        projectTitle.text = activeProject
        updateDisplayTime()
    }

    private fun saveActiveProject() {
        if (activeProject == null) return

        val projectTime =
            if (running)
                SystemClock.elapsedRealtime() - displayTime.getBase()
            else
                elapsedTime

        projectEditor.putLong(activeProject, projectTime)
        projectEditor.commit()
    }

    private fun getProjects() : Map<String, Long> {
        return projectPrefs.getAll() as Map<String, Long>
    }

    private fun updateDisplayTime() {
        displayTime.setBase(SystemClock.elapsedRealtime() - elapsedTime)
    }
}