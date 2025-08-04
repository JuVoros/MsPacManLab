package fs_student;

import game.controllers.PacManController;
import game.core.G;
import game.core.Game;
import java.util.ArrayList;
import java.util.List;



public class MsPacManAgent implements PacManController {
	private enum State {
		COLLECT_PILLS,
		ESCAPE_GHOSTS,
		EAT_GHOSTS,
		TARGET_POWER_PILL,
		BAIT_POWER_PILL
	}

	private State currentState = State.COLLECT_PILLS;
	private final int BAIT_DISTANCE = 10;
	private final int SAFE_DISTANCE = 22;

	public int getAction(Game game, long timeDue) {
		updateState(game);

		switch (currentState) {
            case ESCAPE_GHOSTS:
				return escapeGhosts(game);
			case EAT_GHOSTS:
				return eatGhosts(game);
			case TARGET_POWER_PILL:
				return targetPowerPill(game);
			case BAIT_POWER_PILL:
				return baitPowerPill(game);
			default:
				return collectPills(game);
		}
	}
	private long eatGhostsStartTime = 0;
	private final int EAT_GHOSTS_TIMEOUT = 2000;
	private State previousState = State.COLLECT_PILLS;


	private void updateState(Game game) {
		int current = game.getCurPacManLoc();
		boolean isGhostNearby = false;
		boolean edibleGhostNearby = false;


		int remainingPillsThreshold = 30;
		int remainingPills = getRemainingPillsCount(game);

		for (int i = 0; i < Game.NUM_GHOSTS; i++) {
			int ghostLoc = game.getCurGhostLoc(i);
			if (!game.isEdible(i) && game.getPathDistance(current, ghostLoc) < 10) {
				isGhostNearby = true;
			}
			if (game.isEdible(i) && game.getPathDistance(current, ghostLoc) < SAFE_DISTANCE) {
				edibleGhostNearby = true;
			}
		}


		if (remainingPills <= remainingPillsThreshold) {
			currentState = State.COLLECT_PILLS;
			return;
		}

		if (currentState == State.EAT_GHOSTS && System.currentTimeMillis() - eatGhostsStartTime < EAT_GHOSTS_TIMEOUT) {
			return;
		}

		if (edibleGhostNearby) {
			previousState = currentState;
			currentState = State.EAT_GHOSTS;
			eatGhostsStartTime = System.currentTimeMillis();
		} else if (isGhostNearby) {
			currentState = State.ESCAPE_GHOSTS;
		} else if (shouldBaitPowerPill(game)) {
			currentState = State.BAIT_POWER_PILL;
		} else if (shouldTargetPowerPill(game)) {
			currentState = State.TARGET_POWER_PILL;
		} else {
			currentState = State.COLLECT_PILLS;
		}
	}

	private int getRemainingPillsCount(Game game) {
		int[] pills = game.getPillIndices();
		int count = 0;
		for (int pill : pills) {
			if (game.checkPill(game.getPillIndex(pill))) {
				count++;
			}
		}
		return count;
	}

	private int collectPills(Game game) {
		int[] pills = game.getPillIndices();
		List<Integer> targets = new ArrayList<Integer>();
		for (int pill : pills) {
			if (game.checkPill(game.getPillIndex(pill))) {
				targets.add(pill);
			}
		}

		int[] targetArray = new int[targets.size()];
		for (int i = 0; i < targets.size(); i++) {
			targetArray[i] = targets.get(i);
		}
		return game.getNextPacManDir(game.getTarget(game.getCurPacManLoc(),
				targetArray, true, Game.DM.PATH), true, Game.DM.PATH);
	}

	private int escapeGhosts(Game game) {
		int currentLocation = game.getCurPacManLoc();
		int closestGhost = findClosestGhost(game, currentLocation, false);

		if (closestGhost == -1) {
			return game.getCurPacManDir();
		}

		int[] possibleDirs = game.getPossiblePacManDirs(true);
		int bestDirection = -1;
		int maxDistance = -1;

		for (int dir : possibleDirs) {
			int nextLocation = game.getNeighbour(currentLocation, dir);
			int distanceToGhost = game.getPathDistance(nextLocation, closestGhost);
			if (distanceToGhost > maxDistance) {
				maxDistance = distanceToGhost;
				bestDirection = dir;
			}
		}

		return bestDirection;
	}

	private int eatGhosts(Game game) {
		int currentLocation = game.getCurPacManLoc();
		int closestEdibleGhost = findClosestGhost(game, currentLocation, true);

		if (closestEdibleGhost == -1) {
			return collectPills(game);
		}

		return game.getNextPacManDir(closestEdibleGhost, true, Game.DM.PATH);
	}

	private int targetPowerPill(Game game) {
		int currentLocation = game.getCurPacManLoc();
		int closestPill = findClosestPowerPill(game, currentLocation);

		if (closestPill == -1) {
			return game.getCurPacManDir();
		}

		return game.getNextPacManDir(closestPill, true, Game.DM.PATH);
	}

	private int baitPowerPill(Game game) {
		int currentLocation = game.getCurPacManLoc();
		int closestPill = findClosestPowerPill(game, currentLocation);
		int closestGhost = findClosestGhost(game, currentLocation, false);


		if (closestPill == -1 || closestGhost == -1) {
			return game.getCurPacManDir();
		}


		if (game.getPathDistance(currentLocation, closestGhost) <= BAIT_DISTANCE) {
			return game.getNextPacManDir(closestPill, true, Game.DM.PATH);
		} else {
			return avoidGhosts(game, currentLocation);
		}
	}

	private int findClosestGhost(Game game, int pacManLocation, boolean edibleOnly) {
		int closestGhost = -1;
		int minDistance = Integer.MAX_VALUE;

		for (int i = 0; i < Game.NUM_GHOSTS; i++) {
			if (edibleOnly == game.isEdible(i) && game.getLairTime(i) == 0) {
				int ghostLocation = game.getCurGhostLoc(i);
				int distance = game.getPathDistance(pacManLocation, ghostLocation);
				if (distance < minDistance) {
					minDistance = distance;
					closestGhost = ghostLocation;
				}
			}
		}
		return closestGhost;
	}

	private int findClosestPowerPill(Game game, int pacManLocation) {
		int[] powerPillIndices = game.getPowerPillIndices();
		int closestPill = -1;
		int minDistance = Integer.MAX_VALUE;

		for (int pill : powerPillIndices) {
			if (game.checkPowerPill(pill)) {
				int distance = game.getPathDistance(pacManLocation, pill);
				if (distance < minDistance) {
					minDistance = distance;
					closestPill = pill;
				}
			}
		}
		return closestPill;
	}

	private boolean shouldBaitPowerPill(Game game) {
		int currentLocation = game.getCurPacManLoc();
		int closestPill = findClosestPowerPill(game, currentLocation);
		int closestGhost = findClosestGhost(game, currentLocation, false);


		if (closestPill != -1 && closestGhost != -1 && game.getPathDistance(closestGhost, closestPill) < BAIT_DISTANCE) {
			return true;
		}
		return false;
	}

	private boolean shouldTargetPowerPill(Game game) {
		int currentLocation = game.getCurPacManLoc();

		for (int i = 0; i < Game.NUM_GHOSTS; i++) {
			if (!game.isEdible(i) && game.getLairTime(i) == 0) {
				int ghostLocation = game.getCurGhostLoc(i);
				int distance = game.getPathDistance(currentLocation, ghostLocation);
				if (distance < SAFE_DISTANCE) {
					return true;
				}
			}
		}

		return false;
	}

	private int avoidGhosts(Game game, int pacManLocation) {

		int[] possibleDirs = game.getPossiblePacManDirs(true);
		int bestDirection = game.getCurPacManDir();
		int maxDistance = -1;

		for (int dir : possibleDirs) {
			int nextLocation = game.getNeighbour(pacManLocation, dir);
			int closestGhost = findClosestGhost(game, nextLocation, false);
			int distanceToGhost = game.getPathDistance(nextLocation, closestGhost);
			if (distanceToGhost > maxDistance) {
				maxDistance = distanceToGhost;
				bestDirection = dir;
			}
		}
		return bestDirection;
	}
}

