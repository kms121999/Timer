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
    // Components
    private lateinit var projectSpinner : Spinner
    private lateinit var tvActiveProject : TextView
    private lateinit var projectChronometer : Chronometer
    private lateinit var inputNewProject : EditText

    // Shared Preferences
    private lateinit var projectsFile : SharedPreferences
    private lateinit var projectsEditor : SharedPreferences.Editor

    // State Variables
    private var activeProject : String? = null
    private var elapsedTime : Long = 0
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Components
        projectSpinner = findViewById(R.id.sSelector)

        tvActiveProject = findViewById(R.id.tvActiveProject)
        projectChronometer = findViewById(R.id.cmTimeSpent)
        val buttonStart = findViewById<Button>(R.id.bStart)
        val buttonStop = findViewById<Button>(R.id.bStop)

        inputNewProject = findViewById(R.id.etTitleInput)
        val buttonCreate = findViewById<Button>(R.id.bCreate)

        // Set up Shared Preferences
        projectsFile = getSharedPreferences("projects", Context.MODE_PRIVATE)
        projectsEditor = projectsFile.edit()

        // Used for testing
//        projectsEditor.clear()
//        projectsEditor.commit()

        // Assume there are no existing projects
        // This will be overwritten if the projectSpinner gets populated
        tvActiveProject.text = "Create project below"

        // projectChronometer does not require any setup here
        // inputNewProject does not require any setup here

        // Setup Buttons
        buttonStart.setOnClickListener { startTimer() }
        buttonStop.setOnClickListener { stopTimer() }

        buttonCreate.setOnClickListener { createProject() }

        // Setup Spinner
        projectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                setActiveProject(projectSpinner.selectedItem as String)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // TODO Handle the case when activeProject is deleted.
            }
        }

        populateProjectSpinner()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Save project
        saveActiveProject()
    }

    override fun onPause() {
        super.onPause()

        // Save project
        saveActiveProject()
    }

    /**
     * Start the timer
     */
    private fun startTimer() {
        // Only start if the timer is not running and a project is selected
        if (!running && activeProject != null) {
            // Change the chronometer base appropriately
            updateDisplayTime()
            // Start
            projectChronometer.start()
            running = true
        }
    }

    /**
     * Stop the timer
     */
    private fun stopTimer() {
        // Only stop if not running
        if (running) {
            // Calculate elapsed time
            val stopTime = SystemClock.elapsedRealtime()
            val startTime = projectChronometer.getBase()
            elapsedTime = stopTime - startTime
            // Stop
            projectChronometer.stop()
            running = false
        }
    }

    /**
     * Update chronometer base
     */
    private fun updateDisplayTime() {
        projectChronometer.setBase(SystemClock.elapsedRealtime() - elapsedTime)
    }

    /**
     * Create a new project and make it active
     */
    private fun createProject() {
        val project = inputNewProject.text.toString()

        if (project == "") return

        // Save the new project
        projectsEditor.putLong(project, 0)
        projectsEditor.commit()

        // Repopulate spinner to include new value
        populateProjectSpinner()

        // Spinner should now select the new project
        projectSpinner.setSelection(getProjects().keys.toList().indexOf(project))

        // Clear input field
        inputNewProject.text.clear()
    }

    /**
     * Configure timer for selected project
     */
    private fun setActiveProject(project : String?) {
        // Stop and Save the current project to preference file
        stopTimer()
        saveActiveProject()

        // Switch to project
        activeProject = project
        elapsedTime = getProjects().get(activeProject)!!

        // Update display
        tvActiveProject.text = activeProject
        updateDisplayTime()
    }

    /**
     * Save selected project to Shared Preferences
     */
    private fun saveActiveProject() {
        if (activeProject == null) return

        // If the timer is running, calculate elapsedTime. Otherwise used pre-calculated value
        val projectTime =
            if (running)
                SystemClock.elapsedRealtime() - projectChronometer.getBase()
            else
                elapsedTime

        // Save to Shared Preferences
        projectsEditor.putLong(activeProject, projectTime)
        projectsEditor.commit()
    }

    /**
     * Populate projectSpinner with all existing projects
     */
    private fun populateProjectSpinner() {
        // Get all existing projects
        val projects = getProjects()

        // Populate projectSelector
        ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            projects.keys.toMutableList()
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply adapter
            projectSpinner.adapter = adapter
        }
    }

    /**
     * Get all existing projects
     */
    private fun getProjects() : Map<String, Long> {
        return projectsFile.getAll() as Map<String, Long>
    }
}