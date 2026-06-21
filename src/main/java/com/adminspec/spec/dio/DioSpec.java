package com.adminspec.spec.dio;

import com.adminspec.moves.dio.DioBarrageMove;
import com.adminspec.moves.dio.DioRoadRollerMove;
import com.adminspec.moves.dio.DioSummonMove;
import com.adminspec.moves.dio.DioTimeStopMove;
import com.adminspec.spec.Spec;
import com.adminspec.spec.SpecMove;
import com.adminspec.spec.SpecRegistry;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class DioSpec {
    private DioSpec() {}

    public static void register() {
        Spec spec = new Spec(
            "dio_the_world",
            Component.literal("DIO - The World"),
            Component.literal("The World stand. Muda barrage, time stop, and road roller."),
            List.<SpecMove>of(
                new DioSummonMove(),
                new DioBarrageMove(),
                new DioTimeStopMove(),
                new DioRoadRollerMove()
            )
        );
        SpecRegistry.register(spec);
    }
}
