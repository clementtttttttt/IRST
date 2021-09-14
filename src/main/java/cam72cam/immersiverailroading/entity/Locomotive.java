package cam72cam.immersiverailroading.entity;

import cam72cam.immersiverailroading.Config;
import cam72cam.immersiverailroading.IRItems;
import cam72cam.immersiverailroading.items.ItemRadioCtrlCard;
import cam72cam.immersiverailroading.library.ChatText;
import cam72cam.immersiverailroading.library.KeyTypes;
import cam72cam.immersiverailroading.library.ModelComponentType;
import cam72cam.immersiverailroading.model.part.Control;
import cam72cam.immersiverailroading.registry.LocomotiveDefinition;
import cam72cam.immersiverailroading.util.Speed;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.sync.TagSync;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.serialization.StrictTagMapper;
import cam72cam.mod.serialization.TagField;
import cam72cam.mod.world.World;

import java.util.UUID;

public abstract class Locomotive extends FreightTank {
	private static final float throttleDelta = 0.04f;
	private static final float airBrakeNotch = 0.04f;

	@TagField("deadMansSwitch")
	private boolean deadMansSwitch;
	private int deadManChangeTimeout;

	@TagSync
	@TagField("THROTTLE")
	private float throttle = 0;

	@TagSync
	@TagField("REVERSER")
	private float reverser = 0;

	@TagSync
	@TagField("AIR_BRAKE")
	private float airBrake = 0;

	@TagSync
	@TagField("HORN")
	protected int hornTime = 0;

	@TagSync
	@TagField(value = "HORN_PLAYER", mapper = StrictTagMapper.class)
	protected UUID hornPlayer = null;

	@TagSync
	@TagField("BELL")
	private int bellTime = 0;

	private int bellKeyTimeout;

	/*
	 * 
	 * Stock Definitions
	 * 
	 */
	
	@Override
	public LocomotiveDefinition getDefinition() {
		return super.getDefinition(LocomotiveDefinition.class);
	}

	/*
	 * 
	 * EntityRollingStock Overrides
	 */

	@Override
	public GuiRegistry.EntityGUI guiType() {
		return null;
	}

	@Override
	public void handleKeyPress(Player source, KeyTypes key) {

		if (Config.ImmersionConfig.disableIndependentThrottle) {
			switch (key) {
				case THROTTLE_UP:
					key = KeyTypes.REVERSER_UP;
					break;
				case THROTTLE_ZERO:
					key = KeyTypes.REVERSER_ZERO;
					break;
				case THROTTLE_DOWN:
					key = KeyTypes.REVERSER_DOWN;
					break;
				case REVERSER_UP:
				case REVERSER_ZERO:
				case REVERSER_DOWN:
					return;
			}
		} else if (getDefinition().isLinkedBrakeThrottle()) {
			switch (key) {
				case THROTTLE_UP:
					if (getAirBrake() > 0) {
						key = KeyTypes.AIR_BRAKE_DOWN;
					}
					break;
				case THROTTLE_ZERO:
					setAirBrake(0);
					break;
				case THROTTLE_DOWN:
					if (getThrottle() == 0) {
						key = KeyTypes.AIR_BRAKE_UP;
					}
					break;
				case AIR_BRAKE_UP:
				case AIR_BRAKE_ZERO:
				case AIR_BRAKE_DOWN:
					return;
			}
		}

		switch(key) {
			case HORN:
				setHorn(10, source.getUUID());
				break;
			case BELL:
				if (this.getDefinition().toggleBell) {
					if (bellKeyTimeout == 0) {
						bellTime = bellTime != 0 ? 0 : 10;
						bellKeyTimeout = 10;
					}
				} else {
					setBell(10);
				}
            break;
		case THROTTLE_UP:
			setThrottle(getThrottle() + throttleDelta);
			break;
		case THROTTLE_ZERO:
			setThrottle(0f);
			break;
		case THROTTLE_DOWN:
			setThrottle(getThrottle() - throttleDelta);
			break;
		case REVERSER_UP:
			setReverser(getReverser() + getReverserDelta());
			break;
		case REVERSER_ZERO:
			setReverser(0f);
			break;
		case REVERSER_DOWN:
			setReverser(getReverser() - getReverserDelta());
			break;
		case AIR_BRAKE_UP:
			setAirBrake(getAirBrake() + airBrakeNotch);
			//super.handleKeyPress(source, key);
			break;
		case AIR_BRAKE_ZERO:
			setAirBrake(0f);
			//super.handleKeyPress(source, key);
			break;
		case AIR_BRAKE_DOWN:
			setAirBrake(getAirBrake() - airBrakeNotch);
			//super.handleKeyPress(source, key);
			break;
		case DEAD_MANS_SWITCH:
			if (deadManChangeTimeout == 0) { 
				deadMansSwitch = !deadMansSwitch;
				if (deadMansSwitch) {
					source.sendMessage(ChatText.DEADMANS_SWITCH_ENABLED.getMessage());
				} else {
					source.sendMessage(ChatText.DEADMANS_SWITCH_DISABLED.getMessage());
				}
				this.deadManChangeTimeout = 5;
			}
			break;
			default:
				super.handleKeyPress(source, key);
		}
	}

	protected float getReverserDelta() {
		return throttleDelta;
	}

	public void onDrag(Control component, double delta) {
		super.onDrag(component, delta);
		//System.out.println("DRAG " + component + ": "+ getControlPosition(component));
		switch (component.part.type) {
			case THROTTLE_X:
				setThrottle(getControlPosition(component));
				break;
			case TRAIN_BRAKE_X:
				if (getDefinition().isLinearBrakeControl()) {
					setAirBrake(getControlPosition(component));
				}
				break;
			case REVERSER_X:
				setReverser((0.5f-getControlPosition(component))*2);
				break;
			case THROTTLE_BRAKE_X:
				// value 0     0.5     1
				// throt 0      0      1
				// brake 1      0      0
				setAirBrake(1 - getControlPosition(component)*2);
				setThrottle(getControlPosition(component)*2 - 1);
				break;
		}
	}

	@Override
	public void onDragRelease(Control control) {
		super.onDragRelease(control);
		if (!getDefinition().isLinearBrakeControl() && control.part.type == ModelComponentType.TRAIN_BRAKE_X) {
			setControlPosition(control, 0.5f);
		}
	}

	protected float defaultControlPosition(Control control) {
		switch (control.part.type) {
			case TRAIN_BRAKE_X:
			case THROTTLE_BRAKE_X:
			case REVERSER_X:
				return 0.5f;
		}
		return super.defaultControlPosition(control);
	}

	public ClickResult onClick(Player player, Player.Hand hand) {
		if (player.getHeldItem(hand).is(IRItems.ITEM_RADIO_CONTROL_CARD)) {
			if(this.gauge.isModel() || this.getDefinition().getRadioCapability() || !Config.ConfigBalance.RadioEquipmentRequired) {
				ItemRadioCtrlCard.Data data = new ItemRadioCtrlCard.Data(player.getHeldItem(hand));
				if (player.isCrouching()) {
					player.sendMessage(data.linked == null ? ChatText.RADIO_NOLINK.getMessage() : ChatText.RADIO_UNLINK.getMessage());
					data.linked = null;
				} else {
					player.sendMessage(data.linked == null ? ChatText.RADIO_LINK.getMessage() : ChatText.RADIO_RELINK.getMessage());
					data.linked = this.getUUID();
				}
				data.write();
			}
			else {
				player.sendMessage(ChatText.RADIO_CANT_LINK.getMessage(this.getDefinition().name()));;
			}
			return ClickResult.ACCEPTED;
		}
		return super.onClick(player, hand);
	}
	
	@Override
	public void onTick() {
		super.onTick();
		
		if (getWorld().isServer) {
			for (Control control : getDefinition().getModel().getDraggableComponents()) {
				if (!getDefinition().isLinearBrakeControl() && control.part.type == ModelComponentType.TRAIN_BRAKE_X) {
					setAirBrake(Math.max(0, Math.min(1, getAirBrake() + (getControlPosition(control) - 0.5f) / 8)));
				}
			}

			if (deadManChangeTimeout > 0) {
				deadManChangeTimeout -= 1;
			}
			if (bellKeyTimeout > 0) {
				bellKeyTimeout--;
			}
			
			if (deadMansSwitch && !this.getCurrentSpeed().isZero()) {
				boolean hasDriver = this.getPassengers().stream().anyMatch(Entity::isPlayer);
				if (!hasDriver) {
					this.setThrottle(0);
					this.setAirBrake(1);
				}
			}
			if (hornTime > 0) {
				hornTime--;
			} else if (hornPlayer != null) {
				hornPlayer = null;
			}
			if (bellTime > 0 && !this.getDefinition().toggleBell) {
				bellTime--;
			}
		}

		this.distanceTraveled += simulateWheelSlip();

		if (getWorld().isClient) {
			setControlPosition("REVERSERFORWARD", getReverser() > 0 ? 1 : 0);
			setControlPosition("REVERSERNEUTRAL", getReverser() == 0 ? 1 : 0);
			setControlPosition("REVERSERBACKWARD", getReverser() < 0 ? 1 : 0);
		}
	}

	protected abstract int getAvailableHP();
	
	private double getAppliedTractiveEffort(Speed speed) {
		double locoEfficiency = 0.7f; //TODO config
		double outputHorsepower = Math.abs(Math.pow(getThrottle() * getReverser(), 3) * getAvailableHP());
		
		double tractiveEffortNewtons = (2650.0 * ((locoEfficiency * outputHorsepower) / Math.max(1.4, Math.abs(speed.metric()))));
		return tractiveEffortNewtons;
	}
	
	protected double simulateWheelSlip() {
		double tractiveEffortNewtons = getAppliedTractiveEffort(getCurrentSpeed());
		double staticTractiveEffort = this.getDefinition().getStartingTractionNewtons(gauge) * slipCoefficient(getCurrentSpeed()) * Config.ConfigBalance.tractionMultiplier;
		staticTractiveEffort *= 1.5; // Fudge factor
		double adhesionFactor = tractiveEffortNewtons / staticTractiveEffort;
		if (adhesionFactor > 1) {
			return Math.copySign(Math.min((adhesionFactor-1)/10, 1), getReverser());
		}
		return 0;
	}
	
	public double getTractiveEffortNewtons(Speed speed) {	
		if (!this.isBuilt()) {
			return 0;
		}
		
		double tractiveEffortNewtons = getAppliedTractiveEffort(speed);
		double staticTractiveEffort = this.getDefinition().getStartingTractionNewtons(gauge) * slipCoefficient(speed) * Config.ConfigBalance.tractionMultiplier;
		staticTractiveEffort *= 1.5; // Fudge factor
		
		double adhesionFactor = tractiveEffortNewtons / staticTractiveEffort;
		
		if (adhesionFactor > 1) {
			// CRC Handbook of Physical Quantities. Boca Raton, FL: CRC Press, 1997: 145-156.
			double us = 0.74;
			double uk = 0.57;
			tractiveEffortNewtons = staticTractiveEffort * (uk/us) / adhesionFactor;
		}
		
		if (Math.abs(speed.minecraft()) > this.getDefinition().getMaxSpeed(gauge).minecraft()) {
			tractiveEffortNewtons = 0;
		}
		
		return Math.copySign(tractiveEffortNewtons, getReverser());
	}

	/*
	 * 
	 * Misc Helper functions
	 */
	
	public float getThrottle() {
		return throttle;
	}
	public void setThrottle(float newThrottle) {
		newThrottle = Math.min(1, Math.max(0, newThrottle));
		if (this.getThrottle() != newThrottle) {
			setControlPositions(ModelComponentType.THROTTLE_X, newThrottle);
			throttle = newThrottle;
			triggerResimulate();

			setControlPositions(ModelComponentType.THROTTLE_BRAKE_X, getThrottle()/2 + (1-getAirBrake())/2);
		}
	}

	public float getReverser() {
		return reverser;
	}
	public void setReverser(float newReverser) {
		newReverser = Math.min(1, Math.max(-1, newReverser));

		if (this.getReverser() != newReverser) {
			if (Config.ImmersionConfig.disableIndependentThrottle) {
				// Slave throttle to reverser position
				//setThrottle(Math.abs(newReverser));
				float newThrottle = Math.abs(newReverser);
				setControlPositions(ModelComponentType.THROTTLE_X, newThrottle);
				throttle = newThrottle;
			}
			setControlPositions(ModelComponentType.REVERSER_X, newReverser/-2 + 0.5f);
			reverser = newReverser;
			triggerResimulate();
		}
	}

	public void setHorn(int val, UUID uuid) {
		if (hornPlayer == null && uuid != null) {
			hornPlayer = uuid;
		}
		if (hornPlayer == null || hornPlayer.equals(uuid)) {
			hornTime = val;
		}
	}

	public int getHornTime() {
		return hornTime;
	}

	public Entity getHornPlayer() {
		for (Entity pass : getPassengers()) {
			if (pass.getUUID().equals(hornPlayer)) {
				return pass;
			}
		}
		return null;
	}

	public float getAirBrake() {
		return airBrake;
	}
	public void setAirBrake(float newAirBrake) {
		newAirBrake = Math.min(1, Math.max(0, newAirBrake));
		if (this.getAirBrake() != newAirBrake) {
			if (getDefinition().isLinearBrakeControl()) {
				setControlPositions(ModelComponentType.TRAIN_BRAKE_X, newAirBrake);
			}
			airBrake = newAirBrake;
			triggerResimulate();

			setControlPositions(ModelComponentType.THROTTLE_BRAKE_X, getThrottle()/2 + (1-getAirBrake())/2);
		}
	}
	public int getBell() {
		return bellTime;
	}
	public void setBell(int newBell) {
		this.bellTime = newBell;
	}

	public double slipCoefficient(Speed speed) {
		double slipMult = 1.0;
		World world = getWorld();
		if (world.isPrecipitating() && world.canSeeSky(getBlockPosition())) {
			if (world.isRaining(getBlockPosition())) {
				slipMult = 0.6;
			}
			if (world.isSnowing(getBlockPosition())) {
				slipMult = 0.4;
			}
		}
		// Wheel balance messing with friction
		if (speed.metric() != 0) {
			double balance = 1d/(Math.abs(speed.metric())+100) / (1d/100);
			slipMult *= balance;
		}
		return slipMult;
	}
	
	public float ambientTemperature() {
	    // null during registration
		return internal != null ? getWorld().getTemperature(getBlockPosition()) : 0f;
	}
}
