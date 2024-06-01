# PC - Verão 22/23 - Série de exercícios

Repositório com a resolução das séries do aluno 50542,50546 e 50561.

# Documentação Técnica - Exercício 3 da Série 3

## Descrição Geral

O exercício 3 da série 3 envolve a implementação de um servidor de tópicos usando sockets. O servidor é capaz de lidar com múltiplos clientes, permitindo que eles se inscrevam em tópicos e recebam mensagens publicadas nesses tópicos.

## Detalhes de Implementação

O servidor é implementado na classe `Server`, que usa um `AsynchronousServerSocketChannel` para aceitar conexões de clientes. Cada cliente é representado por uma instância da classe `RemoteClient`, que é iniciada quando uma nova conexão de cliente é aceita.

O servidor mantém um conjunto de clientes ativos (`clientSet`). Este conjunto é usado para gerenciar os clientes ativos e limitar o número de clientes que podem se conectar ao servidor simultaneamente.

O servidor usa uma fila de controle (`controlQueue`) para gerenciar eventos, como novas conexões de clientes, clientes terminando, e mudanças de estado do servidor. Cada evento é representado por uma instância de `ControlMessage`, que é uma interface selada com várias classes de dados que representam diferentes tipos de eventos.

O servidor tem dois loops principais, implementados como corotinas: `acceptLoop` e `controlLoop`. O `acceptLoop` aceita novas conexões de clientes e as coloca na fila de controle. O `controlLoop` processa eventos da fila de controle e realiza ações apropriadas, como iniciar novos clientes, remover clientes que terminaram, e gerenciar mudanças de estado do servidor.

O servidor pode ser iniciado e parado. Quando o servidor é parado, ele fecha o socket do servidor, desliga todos os clientes ativos e muda seu estado para `SHUTTING_DOWN`. Quando todos os clientes terminaram e o loop de aceitação terminou, o estado do servidor muda para `SHUTDOWN`.

## Uso

Para iniciar o servidor, use o método estático `start`, passando o endereço do socket no qual o servidor deve escutar. Para parar o servidor, use o método `shutdown`. Para esperar até que o servidor tenha terminado, use o método `join`.

Os clientes podem se conectar ao servidor. Quando uma nova conexão é aceita, uma nova instância de `RemoteClient` é criada e adicionada ao `clientSet`.

## Dependências de Implementação

Este servidor de tópicos faz uso de estruturas e funções desenvolvidas em exercícios anteriores. Especificamente, ele utiliza:

- A estrutura de fila de mensagens desenvolvida no Exercício 2. Esta estrutura é usada para implementar a `controlQueue`, que gerencia eventos no servidor.
- As funções de extensão assíncronas desenvolvidas no Exercício 1. Estas funções são usadas para lidar com operações assíncronas em `AsynchronousServerSocketChannel` e `AsynchronousSocketChannel`, permitindo que o servidor aceite novas conexões de clientes e leia/escreva dados de/para os sockets de clientes de forma assíncrona.

## ClientSet

O `ClientSet` é uma estrutura de dados utilizada para gerenciar os clientes ativos conectados ao servidor. Ele é implementado como um conjunto (`Set`), onde cada cliente é representado por uma instância da classe `RemoteClient`.

O `ClientSet` é usado para limitar o número de clientes que podem se conectar ao servidor simultaneamente. Quando o limite é atingido, novas conexões de clientes são recusadas até que algum cliente existente seja desconectado.

Além disso, o `ClientSet` é usado para gerenciar o ciclo de vida dos clientes de forma thread-safe. Quando um cliente é desconectado, ele é removido do `ClientSet`. Quando o servidor está sendo desligado, todos os clientes no `ClientSet` são notificados e desconectados.

O `ClientSet` é uma parte crucial da implementação do servidor, pois permite o gerenciamento eficiente dos clientes ativos e garante que o servidor não seja sobrecarregado por um número excessivo de conexões de clientes.