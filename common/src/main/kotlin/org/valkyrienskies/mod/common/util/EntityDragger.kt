package org.valkyrienskies.mod.common.util

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object EntityDragger {
    // How much we decay the addedMovement each tick after player hasn't collided with a ship for at least 10 ticks.
    private const val ADDED_MOVEMENT_DECAY = 0.9

    /**
     * Drag these entities with the ship they're standing on.
     */
    fun dragEntitiesWithShips(entities: Iterable<Entity>) {
        entities.forEach { entity ->
            val entityDraggingInformation = (entity as IEntityDraggingInformationProvider).draggingInformation

            var dragTheEntity = false
            var addedMovement: Vector3dc? = null
            var addedYRot = 0.0

            val shipDraggingEntity = entityDraggingInformation.lastShipStoodOn

            // Only drag entities that aren't mounted to vehicles
            if (shipDraggingEntity != null && entity.vehicle == null) {
                if (entityDraggingInformation.isEntityBeingDraggedByAShip()) {
                    // Compute how much we should drag the entity
                    val shipData = entity.level().shipObjectWorld.allShips.getById(shipDraggingEntity)
                    if (shipData != null) {
                        dragTheEntity = true

                        // region Compute position dragging
                        val newPosIdeal = shipData.shipToWorld.transformPosition(
                            shipData.prevTickTransform.worldToShip.transformPosition(
                                Vector3d(entity.x, entity.y, entity.z)
                            )
                        )
                        addedMovement = Vector3d(
                            newPosIdeal.x - entity.x,
                            newPosIdeal.y - entity.y,
                            newPosIdeal.z - entity.z
                        )
                        // endregion

                        // region Compute look dragging
                        val yViewRot = entity.getViewYRot(1.0f).toDouble()
                        // Get the y-look vector of the entity only using y-rotation, ignore x-rotation
                        val entityLookYawOnly =
                            Vector3d(sin(-Math.toRadians(yViewRot)), 0.0, cos(-Math.toRadians(yViewRot)))

                        val newLookIdeal = shipData.shipToWorld.transformDirection(
                            shipData.prevTickTransform.worldToShip.transformDirection(
                                entityLookYawOnly
                            )
                        )

                        // Get the X and Y rotation from [newLookIdeal]
                        val newXRot = asin(-newLookIdeal.y())
                        val xRotCos = cos(newXRot)
                        val newYRot = -atan2(newLookIdeal.x() / xRotCos, newLookIdeal.z() / xRotCos)

                        // The Y rotation of the entity before dragging
                        var entityYRotCorrected = entity.yRot % 360.0
                        // Limit [entityYRotCorrected] to be between -180 to 180 degrees
                        if (entityYRotCorrected < -180.0) entityYRotCorrected += 360.0
                        if (entityYRotCorrected > 180.0) entityYRotCorrected -= 360.0

                        // The Y rotation of the entity after dragging
                        val newYRotAsDegrees = Math.toDegrees(newYRot)
                        // Limit [addedYRotFromDragging] to be between -180 to 180 degrees
                        var addedYRotFromDragging = newYRotAsDegrees - entityYRotCorrected
                        if (addedYRotFromDragging < -180.0) addedYRotFromDragging += 360.0
                        if (addedYRotFromDragging > 180.0) addedYRotFromDragging -= 360.0

                        addedYRot = addedYRotFromDragging
                        // endregion
                    }
                } else {
                    dragTheEntity = true
                    addedMovement = entityDraggingInformation.addedMovementLastTick
                        .mul(ADDED_MOVEMENT_DECAY, Vector3d())
                    addedYRot = entityDraggingInformation.addedYawRotLastTick * ADDED_MOVEMENT_DECAY
                }
            }

            if (dragTheEntity && addedMovement != null && addedMovement.isFinite && addedYRot.isFinite()) {
                // TODO: Do collision on [addedMovement], as currently this can push players into
                //       blocks
                // Apply [addedMovement]
                val newBB = entity.boundingBox.move(addedMovement.toMinecraft())
                entity.boundingBox = newBB
                entity.setPos(
                    entity.x + addedMovement.x(),
                    entity.y + addedMovement.y(),
                    entity.z + addedMovement.z()
                )
                entityDraggingInformation.addedMovementLastTick = addedMovement

                // Apply [addedYRot]
                // Don't apply it to server players to fix rotation of placed blocks
                if (addedYRot.isFinite() && entity !is ServerPlayer) {
                    entity.yRot += addedYRot.toFloat()
                    entity.yHeadRot += addedYRot.toFloat()
                    entityDraggingInformation.addedYawRotLastTick = addedYRot
                }
            }
            entityDraggingInformation.ticksSinceStoodOnShip++
            entityDraggingInformation.mountedToEntity = entity.vehicle != null
        }
    }
}
