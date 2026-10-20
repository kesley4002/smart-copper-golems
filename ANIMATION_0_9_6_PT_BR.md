# Smart Copper Golems Extended 0.9.6 — melhoria da animação

O golem continua usando o próprio renderizador Java Vanilla com animações adicionais apenas para body, left_leg e right_leg; o pacote não precisa de EMF nem ETF.

**Mudanças desta revisão experimental:**
- Baús filtrados a 2, 3, 4 ou 5 blocos acima do golem iniciam o crescimento quando o golem se aproxima horizontalmente a cerca de três blocos, sem aguardar 100 ticks travado.
- A progressão da animação é limitada à altura: dois blocos = 25%, três = 50%, quatro = 75%, cinco = 100% da animação original do usuário.
- A sincronização Fabric inclui um novo inteiro `maxProgress` no payload; **instalar a mesma versão 0.9.6 no servidor e nos clientes Java**, sem misturar com 0.9.5.
- Os ângulos de rotação das pernas são zerados durante a extensão para impedir que a animação vanilla de caminhada deixe as pernas abertas.
- O golem interrompe sua navegação durante crescer, interagir e encolher; volta a andar apenas quando as pernas voltam ao normal.
- Como fallback para depósitos remotos *não elevados*, o reconhecimento de travamento foi reduzido de 100 para 20 ticks, sem desativar as verificações de filtros e de travessia por pisos sólidos.

**Limitações a testar:** A altura percentual é aproximada para os pivôs específicos do modelo. O movimento ainda tem duração 5,05 s para crescer e 5,05 s para encolher; alturas menores movem as pernas por uma fração da animação, no mesmo tempo. Somente clientes Java Fabric visualizam por enquanto; Bedrock/Geyser não recebe os eventos customizados automaticamente.

**Teste:** Faça backup do mundo. Verifique baús a 2, 3, 4 e 5 blocos de altura e baús acessíveis em escadas, além de baús no chão e baús atrás de paredes. Confirme que as pernas ficam paralelas, a tampa abre, os itens não duplicam e o golem não anda antes de encolher. O mod segue em PR experimental, não mesclar ou distribuir release sem confirmação em jogo.
