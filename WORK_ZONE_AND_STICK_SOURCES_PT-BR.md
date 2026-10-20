# Smart Copper Golems Extended 0.8.0 — área de trabalho, entradas com graveto e abrigo noturno

## Configuração do servidor

Execute como OP:

```mcfunction
/scg radius 500
```

O argumento é a **largura total do quadrado em blocos**, não um raio radial: 500 implica 250 blocos para cada lado em X/Z a partir do centro de cada golem. Aceita larguras pares de 16 a 1000; `/scg radius` mostra a configuração atual. A largura é **global ao mundo**; o centro é **individual para cada golem**.

O primeiro local registrado para cada golem pelo servidor é mantido por UUID no arquivo `smart-copper-golem-work-zones.properties`, dentro da pasta do mundo. Para golems criados com o mod já instalado, o ponto é capturado quando a entidade entra no mundo. Para golems anteriores à instalação, o local em que forem encontrados pela primeira vez após instalar o mod se torna o centro. O arquivo também conserva a largura escolhida após reiniciar.

**Limites:** o golem nunca força carregamento de chunks para buscar baús; somente os chunks já carregados participam. O alcance vertical de busca é o configurado anteriormente (8 blocos em torno da altura atual), não 500 blocos verticais. Buscar em uma área grande não garante que caminhos longos/fechados sejam alcançáveis; é necessário acesso caminhável. O filtro de categorias e a animação não mudam.

## Entradas personalizadas por moldura

- Moldura comum com um **graveto** (`minecraft:stick`) em **0°** em baú comum, baú duplo ou barril: esse recipiente passa a ser **origem** dos itens. O golem retira itens e leva a recipientes de destino filtrados.
- Molduras em 45° continuam definindo categoria. Graveto a 45° continua representando **marcenaria**.
- Molduras com outros itens em 0° continuam definindo destino exato.
- Baú de cobre continua funcionando como origem original.
- Um recipiente marcado como origem é excluído das listas de destinos, inclusive fallback, evitando retorno para o mesmo baú.
- Em baús duplos, uma moldura anexada a qualquer metade é reconhecida para o inventário inteiro.
- Em barris, a moldura pode ficar em qualquer face, como já ocorria nos filtros.
- Se houver várias origens marcadas, o golem escolhe as disponíveis dentro da área segundo sua pesquisa. Não é necessário transferir manualmente pilhas a um baú de cobre.

## Rotina noturna

No intervalo de noite do ciclo de dias vanilla (ticks 13000 a 23000), um golem **sem item na mão e sem transporte em andamento** recebe ordem de caminhar de volta ao centro. Um golem carregando itens tenta terminar o transporte antes de retornar. Ele não é teleportado, não atravessa paredes e não garante segurança se a área ao redor do ponto de nascimento não for um abrigo.

## Testes manuais no servidor

1. Em mundo de teste, OP: `/scg radius 500`; conferir resposta e persistência após reinício.
2. Criar novo golem no interior da base, colocar baú externo contendo itens com moldura de graveto em 0° e observar coleta e organização em destinos filtrados.
3. Repetir com barril como origem e com baú duplo; confirmar que nenhuma pilha é duplicada/perdida.
4. Testar graveto a 45° em outro recipiente para confirmar que ainda funciona como filtro de marcenaria.
5. Testar origem com moldura retirada durante o percurso; verificar que o golem não cria itens nem pega de recipientes sem marcador.
6. Testar baú de cobre como origem da maneira anterior.
7. Testar vários golems próximos e recipientes com filtros; assegurar que a origem marcada nunca recebe depósitos.
8. Testar recipientes a mais de 32 blocos e próximos à borda do quadrado de trabalho. Acessos longos precisam ser caminháveis.
9. Verificar retorno ao local inicial à noite, inclusive a situação em que o golem já está carregando um item. Construir abrigo no centro.
10. Testar Java dedicado e Geyser/Floodgate. Comparar TPS quando a área contém vários chunks carregados.

**Aviso:** versão experimental. Um build Gradle aprovado não substitui os testes reais de movimentação/segurança da base.
