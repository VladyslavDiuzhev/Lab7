package server.commands;

import core.precommands.IdPrecommand;
import core.precommands.Precommand;
import server.commands.interfaces.Command;
import server.commands.interfaces.IdCommand;
import core.essentials.Vehicle;
import core.interact.Message;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Класс команды получения информации об элементе по id.
 *
 * @author Владислав Дюжев
 * @version 1.0
 */
public class InfoById implements Command, IdCommand {
    private final String argument;

    public InfoById(ArrayList<String> args) {
        this.argument = args.get(0);
    }

    public InfoById(Precommand precommand) {
        this.argument = ((IdPrecommand) precommand).getId();
    }

    @Override
    public Message execute(Stack<Vehicle> stack) {
        int index = idArgToIndex(argument, stack);
        if (index == -1) {
            return new Message("Неверный аргумент. Ожидается число (id). Или данного элемента не существует.", true);
        }
        Vehicle vehicle = stack.get(index);
        String info = String.format("id: %d \n" +
                        "Название: %s \n" +
                        "Тип: %s \n" +
                        "Дата создания: %s \n" +
                        "Мощность: %s \n" +
                        "Тип топлива: %s \n" +
                        "Координаты: %s", vehicle.getId(), vehicle.getName(), vehicle.getType(),
                vehicle.getCreationDate(), vehicle.getEnginePower(), vehicle.getFuelType(),
                vehicle.getCoordinates());
        return new Message(info, true);
    }
}
