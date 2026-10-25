# Smart Copper Golems Extended 0.9.23 (Fabric / Minecraft 26.2)

## Modo opcional: somente meus baús / private-only storage

Na 0.9.22, a regra do baú nomeado proibia outros golems de usarem o baú de Roger, mas **o próprio Roger ainda podia usar qualquer baú sem nome (compartilhado)**.

Na 0.9.23 é possível limitar individualmente um golem a trabalhar **somente nos recipientes com seu próprio nome**, sem mudar o comportamento dos demais. Configuração salva por UUID em `smart-copper-golem-private-storage.properties`. Padrão após instalar: modo compartilhado, igual à 0.9.22.

### Exemplos de comandos para Roger

- `/copper_golem Roger bau exclusivo` — ativa acesso somente a baús/barris nomeados Roger.
- `/copper_golem Roger bau compartilhado` — volta ao padrão: seu baú Roger + baús/barris sem nome.
- `/copper_golem Roger bau` — consulta estado.
- `/copper_golem Roger storage private` — comando equivalente em inglês.
- `/copper_golem Roger storage shared` — restaura modo compartilhado em inglês.
- `/scg golem "Coletor: Roger" storage private` — mesma configuração pelo prefixo /scg.
- Também aceita o nome completo `"Coletor: Roger"`; se existirem dois golems com o mesmo apelido, o comando recusa a operação até corrigir os nomes duplicados.

### Regras

| Baú ou barril | Roger em modo compartilhado | Roger em modo exclusivo | Outros golems |
|---|---|---|---|
| **Nomeado Roger** com moldura de cana | Sim | **Sim** | Não |
| **Sem nome** com moldura de cana | Sim | **Não** | Sim |
| **Nomeado Pedro** com moldura de cana | Não | Não | Somente Pedro |
| **Nomeado Roger**, mas sem moldura de cana | Não para cana | Não para cana | Não |

- As molduras continuam indicando categorias, exatamente como antes. Uma moldura de cana em **45°** filtra só `minecraft:sugar_cane`.
- O modo exclusivo vale para **todas** as interações de inventário dos golems, incluindo item frames, depósitos altos, fontes com stick, compostagem e carvão da profissão Fornalheiro, pois é integrado à mesma verificação de propriedade da versão anterior.
- **Baús duplos** continuam protegidos: basta uma metade nomeada Roger, desde que a outra não seja nomeada com outro proprietário.
- Se o único baú pessoal estiver cheio ou fora do raio, **Roger não tentará um baú compartilhado** enquanto o modo exclusivo estiver ativo. Ele pode ficar com o item na mão até surgir um destino válido. Não há descarte intencional nem roteamento a baú de terceiros.
- Os outros golems continuam usando os recipientes compartilhados normalmente. **O modo exclusivo não é global**.
- Jogadores, funis e mecanismos vanilla não são bloqueados. Esta é uma limitação da IA dos Copper Golems.
- Mod para **Minecraft 26.2**. O port do Minecraft 26.3 foi mantido intacto.

## Teste recomendado
1. Configure uma Name Tag `Coletor: Roger` e um barril com nome de bigorna `Roger` + moldura cana 45°. Coloque também um baú sem nome com a mesma moldura e um golem `Coletor: Pedro`.
2. Use `/copper_golem Roger bau exclusivo`. Jogue cana no chão perto de Roger; ele deve usar **apenas o barril Roger**, mesmo que o compartilhado esteja mais perto.
3. Encha o barril Roger e deixe o compartilhado vazio; o golem não deve colocar nada no compartilhado.
4. Verifique que Pedro continua podendo usar o compartilhado e não pode usar o baú Roger.
5. Use `/copper_golem Roger bau compartilhado`. Agora Roger poderá usar novamente o baú sem nome.
6. Reinicie o servidor e confirme que o modo escolhido permanece.
7. Teste baú duplo, limite diurno/noturno, baú em altura, troca de nome, Coletor, Agricultor e Fornalheiro. Teste Bedrock via Geyser em uma cópia do mundo.

**Experimental:** validação automática não substitui o teste real dentro do Minecraft.
