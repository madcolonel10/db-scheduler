package com.github.kagkarlsson.scheduler;

import com.github.kagkarlsson.scheduler.task.TaskInstance;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Execution {
	public final TaskInstance taskInstance;
	public final LocalDateTime executionTime;
	public boolean picked;
	public String pickedBy;
	public LocalDateTime lastHeartbeat;

	public Execution(LocalDateTime executionTime, TaskInstance taskInstance) {
		this(executionTime, taskInstance, false, null, null);
	}

	public Execution(LocalDateTime executionTime, TaskInstance taskInstance, boolean picked, String pickedBy, LocalDateTime lastHeartbeat) {
		this.executionTime = executionTime;
		this.taskInstance = taskInstance;
		this.picked = picked;
		this.pickedBy = pickedBy;
		this.lastHeartbeat = lastHeartbeat;
	}

	public void setPicked(String pickedBy, LocalDateTime timePicked) {
		this.pickedBy = pickedBy;
		this.picked = true;
		this.lastHeartbeat = timePicked;
	}

	public LocalDateTime getExecutionTime() {
		return executionTime;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Execution execution = (Execution) o;
		return Objects.equals(executionTime, execution.executionTime) &&
				Objects.equals(taskInstance, execution.taskInstance);
	}

	@Override
	public int hashCode() {
		return Objects.hash(executionTime, taskInstance);
	}


	@Override
	public String toString() {
		return "Execution: " +
				"task=" + taskInstance.getTask().getName() +
				", id=" + taskInstance.getId() +
				", executionTime=" + executionTime;
	}
}