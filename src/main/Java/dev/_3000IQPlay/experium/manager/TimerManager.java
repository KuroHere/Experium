package dev._3000IQPlay.experium.manager;

import dev._3000IQPlay.experium.Experium;
import dev._3000IQPlay.experium.features.Feature;
import dev._3000IQPlay.experium.features.modules.player.TimerSpeed;

public class TimerManager
        extends Feature {
    private float timer = 1.0f;
    private TimerSpeed module;

    public void init() {
        this.module = Experium.moduleManager.getModuleByClass(TimerSpeed.class);
    }

    public void unload() {
        this.timer = 1.0f;
        TimerManager.mc.timer.tickLength = 50.0f;
    }

    public void update() {
        if (this.module != null && this.module.isEnabled()) {
            this.timer = this.module.speed;
        }
        TimerManager.mc.timer.tickLength = 50.0f / (this.timer <= 0.0f ? 0.1f : this.timer);
    }

    public float getTimer() {
        return this.timer;
    }

    public void setTimer(float timer) {
        if (timer > 0.0f) {
            this.timer = timer;
        }
    }

    @Override
    public void reset() {
        this.timer = 1.0f;
    }
}

