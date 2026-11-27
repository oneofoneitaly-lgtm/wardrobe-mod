package it.adryh.wardrobe;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

public final class Parts {
    private Parts() {}

    public static ModelPart of(HumanoidModel<?> model, Part part) {
        return switch (part) {
            case HEAD -> model.head;
            case BODY -> model.body;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
        };
    }
}
