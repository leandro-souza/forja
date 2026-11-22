package br.com.forja.core;

/**
 * Exercicio do catalogo.
 * @param spine carga sobre a coluna: 1 = tronco apoiado, 2 = moderada, 3 = alta (evitado em quem relata dor)
 */
public record Exercise(String code, String name, String group, String img, boolean compound, int spine, String cue) {}
