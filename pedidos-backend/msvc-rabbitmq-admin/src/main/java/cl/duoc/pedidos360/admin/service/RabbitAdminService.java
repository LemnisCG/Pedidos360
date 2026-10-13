package cl.duoc.pedidos360.admin.service;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

@Service
public class RabbitAdminService {

    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminService(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    public void createQueue(String name) {
        Queue queue = new Queue(name, true);
        rabbitAdmin.declareQueue(queue);
    }

    public void deleteQueue(String name) {
        rabbitAdmin.deleteQueue(name);
    }

    public void createExchange(String name) {
        DirectExchange exchange = new DirectExchange(name, true, false);
        rabbitAdmin.declareExchange(exchange);
    }

    public void deleteExchange(String name) {
        rabbitAdmin.deleteExchange(name);
    }

    public void createBinding(String queueName, String exchangeName, String routingKey) {
        Queue queue = new Queue(queueName, true);
        DirectExchange exchange = new DirectExchange(exchangeName, true, false);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(routingKey);
        rabbitAdmin.declareBinding(binding);
    }

    public void deleteBinding(String queueName, String exchangeName, String routingKey) {
        Binding binding = new Binding(queueName, Binding.DestinationType.QUEUE, exchangeName, routingKey, null);
        rabbitAdmin.removeBinding(binding);
    }
}
