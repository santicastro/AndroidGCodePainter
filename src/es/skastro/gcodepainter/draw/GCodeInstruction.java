package es.skastro.gcodepainter.draw;

public class GCodeInstruction {

    Command command;
    Point point;

    public enum Command {
        G1, G21, G90
    }

    public GCodeInstruction(Command command, Point point) {
        super();
        this.command = command;
        this.point = point;
    };

    @Override
    public String toString() {
        return command.name() + ((point != null) ? (" " + point.toString()) : "");
    }
}
