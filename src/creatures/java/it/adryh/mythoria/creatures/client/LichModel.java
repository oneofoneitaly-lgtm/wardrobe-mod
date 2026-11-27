package it.adryh.mythoria.creatures.client;

import it.adryh.mythoria.creatures.entity.ShadowLich;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** It floats: no legs, a swaying robe and crystals circling around. */
final class LichModel extends HierarchicalModel<ShadowLich> {
    private static final float RAD = Mth.PI / 180.0F;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart torso;
    private final ModelPart skirt;
    private final ModelPart cape;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart orbit;

    LichModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        torso = root.getChild("torso");
        skirt = root.getChild("skirt");
        cape = root.getChild("cape");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        orbit = root.getChild("orbit");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(ShadowLich entity, float swing, float amount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float partial = Mth.clamp(age - entity.tickCount, 0.0F, 1.0F);
        head.yRot += Mth.clamp(yaw, -60.0F, 60.0F) * RAD;
        head.xRot += pitch * RAD * 0.6F;

        float hover = Mth.sin(age * 0.07F) * 1.2F;
        for (ModelPart part : new ModelPart[]{head, torso, skirt, cape, rightArm, leftArm, orbit}) {
            part.y += hover;
        }
        float move = Math.min(amount, 1.0F);
        skirt.xRot += Mth.sin(age * 0.09F) * 0.05F + move * 0.3F;
        cape.xRot += Mth.sin(age * 0.08F) * 0.06F + move * 0.45F;
        rightArm.zRot += Mth.sin(age * 0.06F) * 0.05F;
        leftArm.zRot -= Mth.sin(age * 0.06F) * 0.05F;
        orbit.yRot = age * 0.06F;

        if (entity.castTicks > 0) {
            float t = Mth.sin(Mth.clamp((16.0F - (entity.castTicks - partial)) / 16.0F, 0.0F, 1.0F) * Mth.PI);
            leftArm.xRot -= 2.2F * t;
            rightArm.xRot -= 1.1F * t;
            orbit.yRot += t * 2.0F;
        }
    }
}
