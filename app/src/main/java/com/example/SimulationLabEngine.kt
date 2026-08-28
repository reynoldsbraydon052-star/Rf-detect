package com.example

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class SimulationScenario {
    NORMAL_ENVIRONMENT,
    NEW_SIGNAL,
    INTERMITTENT_SIGNAL,
    FREQUENCY_DRIFT,
    FREQUENCY_HOPPING,
    MULTIPLE_SIGNALS,
    ENVIRONMENTAL_CHANGE,
    BEHAVIOR_CHANGE,
    SIGNAL_DISAPPEARANCE,
    CORRELATED_SENSOR_EVENT
}

class SimulationLabEngine(
    private val signalRadarViewModel: SignalRadarViewModel
) {
    private var simulationJob: Job? = null
    
    private val _activeScenario = MutableStateFlow<SimulationScenario?>(null)
    val activeScenario: StateFlow<SimulationScenario?> = _activeScenario.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun startSimulation(scenario: SimulationScenario, seed: Long = System.currentTimeMillis()) {
        stopSimulation()
        _activeScenario.value = scenario
        _isRunning.value = true
        
        val random = Random(seed)

        simulationJob = CoroutineScope(Dispatchers.Default).launch {
            var step = 0
            while (isActive) {
                val blips = generateScenarioBlips(scenario, step, random)
                blips.forEach { blip ->
                    signalRadarViewModel.injectSimulationBlip(blip)
                }
                delay(1000)
                step++
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        _isRunning.value = false
        _activeScenario.value = null
    }

    private fun generateScenarioBlips(scenario: SimulationScenario, step: Int, random: Random): List<RadarBlip> {
        val blips = mutableListOf<RadarBlip>()
        
        when (scenario) {
            SimulationScenario.NORMAL_ENVIRONMENT -> {
                blips.add(createBlip("SIM_WIFI_ROUTER", "WIFI", 15.0f, 45f, random))
                blips.add(createBlip("SIM_BLE_BEACON", "BLE", 5.0f, 120f, random))
            }
            SimulationScenario.NEW_SIGNAL -> {
                blips.add(createBlip("SIM_WIFI_ROUTER", "WIFI", 15.0f, 45f, random))
                if (step > 5) {
                    blips.add(createBlip("SIM_UNKNOWN_DEVICE", "WIFI", 8.0f, 200f, random))
                }
            }
            SimulationScenario.INTERMITTENT_SIGNAL -> {
                blips.add(createBlip("SIM_WIFI_ROUTER", "WIFI", 15.0f, 45f, random))
                if (step % 4 < 2) {
                    blips.add(createBlip("SIM_INTERMITTENT", "BLE", 3.0f, 90f, random))
                }
            }
            SimulationScenario.FREQUENCY_DRIFT -> {
                val driftFreq = 2400.0 + (step * 5.0)
                blips.add(createBlip("SIM_DRIFTER", "BLE", 10.0f, 180f, random).copy(frequencyMhz = driftFreq))
            }
            SimulationScenario.FREQUENCY_HOPPING -> {
                val hopFreq = if (step % 2 == 0) 2412.0 else 2462.0
                blips.add(createBlip("SIM_HOPPER", "WIFI", 12.0f, 270f, random).copy(frequencyMhz = hopFreq))
            }
            SimulationScenario.MULTIPLE_SIGNALS -> {
                for (i in 0..4) {
                    blips.add(createBlip("SIM_DEV_$i", "BLE", 2.0f + i*3f, (i*70f)%360f, random))
                }
            }
            SimulationScenario.ENVIRONMENTAL_CHANGE -> {
                if (step < 10) {
                    blips.add(createBlip("SIM_ENV_1", "WIFI", 10.0f, 0f, random))
                } else {
                    blips.add(createBlip("SIM_ENV_2", "WIFI", 15.0f, 90f, random))
                }
            }
            SimulationScenario.BEHAVIOR_CHANGE -> {
                val distance = if (step < 8) 20.0f else 5.0f
                blips.add(createBlip("SIM_BEHAVIOR", "WIFI", distance, 45f, random))
            }
            SimulationScenario.SIGNAL_DISAPPEARANCE -> {
                if (step < 10) {
                    blips.add(createBlip("SIM_FADING", "BLE", 5.0f + (step*0.5f), 180f, random))
                }
            }
            SimulationScenario.CORRELATED_SENSOR_EVENT -> {
                blips.add(createBlip("SIM_RF_SOURCE", "WIFI", 10.0f, 45f, random))
                if (step > 3 && step < 8) {
                    blips.add(createBlip("SIM_MAGNETIC_SPIKE", "MAGNETIC", 10.0f, 45f, random))
                }
            }
        }
        
        return blips
    }

    private fun createBlip(name: String, type: String, dist: Float, angle: Float, random: Random): RadarBlip {
        return RadarBlip(
            id = name,
            name = name,
            distance = dist + (random.nextFloat() * 0.5f - 0.25f),
            targetAngleOffset = angle + (random.nextFloat() * 2f - 1f),
            type = type,
            rssi = -50 - (dist * 2).toInt(),
            provenance = DataProvenance.SIMULATED
        )
    }
}
