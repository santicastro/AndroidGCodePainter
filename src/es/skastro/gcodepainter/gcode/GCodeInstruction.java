package es.skastro.gcodepainter.gcode;

import es.skastro.gcodepainter.draw.util.PointFUtils;

public class GCodeInstruction {

    Command command;
    PointFUtils point;

    public enum Command {
        G1, G21, G90
    }

    public GCodeInstruction(Command command, PointFUtils point) {
        super();
        this.command = command;
        this.point = point;
    };

    @Override
    public String toString() {
        return command.name() + ((point != null) ? (" " + point.toString()) : "");
    }
}
