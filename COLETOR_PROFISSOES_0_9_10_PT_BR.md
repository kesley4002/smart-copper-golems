# Smart Copper Golems Extended — profissões, etapa 1: Coletor

Base: feature/named-golem-night-shifts-26.2 (versão de áreas e turnos testada pelo usuário).

## Name Tags
- "Coletor" -> profissão Coletor, sem nome individual.
- "Coletor: João" -> profissão Coletor, nome individual João.
- "João" ou golem sem etiqueta -> Organizador original.
- "Agricultor: Pedro", "Fornalheiro" ou nomes de profissões ainda não implementadas continuam agindo como Organizador; não há função dessas profissões neste protótipo.
- Comandos individuais usam o nome completo, com aspas quando há espaços: /copper_golem "Coletor: João" radius 30

## Coletor — comportamento adicionado
1. Detecta apenas entidades de itens já caídos num alcance local de 16 blocos na horizontal e 5 na vertical. Não busca em chunks descarregados.
2. Somente seleciona itens com caminho alcançável e baú/barril de destino carregado, com moldura adequada (0 grau categoria, 45 grau item), espaço e dentro da área de trabalho do golem.
3. Anda até o item, recolhe no máximo a capacidade configurada por /scg carry (16, 32 ou 64) e conserva no chão o excedente.
4. Transporta usando a mão, rotas, filtros, sons, animações e travas de recipiente já existentes.
5. Não coleta drops sem destino de armazenamento válido. Se destino lotar durante transporte, o item continua seguro na mão para tentar novamente.
6. O Coletor não retira itens de baús de origem; a profissão Organizador continua responsável pelo trabalho antigo.
7. Retirar ou trocar a etiqueta faz o golem voltar à função de Organizador, sem limpar os limites de raio associados ao UUID.

## Pendências
- A IA não colhe nem quebra plantações, não planta, não compostagem, não abastece fornos e não fabrica carvão vegetal.
- Não adiciona patrulha de longa distância: o Coletor procura drops até 16 blocos da própria posição; a área individual ainda limita os destinos.
- A compatibilidade de animações de coleta do cliente Bedrock via Geyser deverá ser testada no jogo.
- A compilação automatizada não garante ausência de regressões em situações de corrida, movimentação e carregamento de chunks.

## Testes em cópia do mundo
1. Marcar baú de destino com moldura e deixar espaços livres; posicionar itens dropados a 5 blocos de um golem "Coletor: João".
2. Observar caminhada, coleta física (remoção do drop), item na mão e depósito no baú filtrado.
3. Repetir sem filtro de destino e com baú cheio: não deve coletar itens sem lugar.
4. Repetir com pilha maior que /scg carry: verificar o excedente no chão.
5. Repetir com dois Coletores e um Organizador normal; procurar colisões e duplicações.
6. Testar área individual, trabalho noturno, reinicialização e animações em baús elevados.
7. Confirmar 42 categorias / 1413 itens e filtros antigos.

Não mesclar ou substituir o mod do mundo principal sem validação em jogo. Manter o JAR da versão anterior como rollback.
