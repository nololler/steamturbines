The easiest way to describe this to an agent is to stop thinking in terms of absolute world directions and instead define a **local coordinate system** for the block.

From your description, the block has:

* A primary axis = `FACING`
* Two shaft connections located on the block's local left/right axis
* A GUI button located on the block's local top/bottom axis

The problem is that when `FACING` becomes `UP` or `DOWN`, the game no longer knows how to orient the block's local X/Y axes unless you provide an additional rotation value.

---

## What the agent should understand

### Horizontal placements

When facing:

* NORTH
* SOUTH
* EAST
* WEST

The local axes are obvious:

| Local Axis | NORTH | SOUTH | EAST  | WEST  |
| ---------- | ----- | ----- | ----- | ----- |
| Forward    | North | South | East  | West  |
| Right      | East  | West  | South | North |
| Up         | Up    | Up    | Up    | Up    |

Result:

* Shafts = Local Left + Local Right
* GUI = Local Up + Local Down

This works correctly.

---

### Vertical placements

When facing:

* UP
* DOWN

The block loses information about its rotation around the facing axis.

Example:

A block facing UP can be oriented like:

```
Case A:
Front = UP
Top = NORTH

Case B:
Front = UP
Top = EAST

Case C:
Front = UP
Top = SOUTH

Case D:
Front = UP
Top = WEST
```

All four are valid orientations.

Minecraft's `Direction` alone cannot distinguish these.

---

## The real issue

Your block actually needs:

```java
Direction facing;
int rotation;
```

or

```java
Direction facing;
Direction horizontalFacing;
```

similar to how:

* Create pumps
* Encased fans
* Mechanical mixers
* Cogwheel-based blocks

handle vertical placement.

When the block is facing UP or DOWN, you must store an additional rotation state.

---

## Describe it to the agent like this

```md
The block has three local axes:

- Forward = FACING
- Right = perpendicular shaft axis
- Up = GUI axis

The shafts should always appear on the block's local left/right sides.

The GUI button should always appear on the block's local top/bottom sides.

For NORTH/SOUTH/EAST/WEST facings this is straightforward.

For UP/DOWN facings an additional rotation state is required because FACING alone does not define the block's orientation around its own axis.

Example:

FACING=UP
ROTATION=NORTH

Local Right = EAST
Local Up = NORTH

FACING=UP
ROTATION=EAST

Local Right = SOUTH
Local Up = EAST

Without storing ROTATION, Minecraft cannot determine where the shafts or GUI should appear when the block faces UP or DOWN.

The bug is not a shaft rendering problem. It is that the block lacks a second orientation property for vertical placements.
```

---

If your pump is behaving like Create's Mechanical Pump, I'd actually tell the agent to look at how Create handles:

* `DirectionalAxisKineticBlock`
* `CombinedOrientation`
* `VerticalOrientation`
* placement logic for pumps and fans

because what you're describing is essentially a **6-direction block that also needs roll/rotation information when vertical**, not just a simple `FACING` block.
