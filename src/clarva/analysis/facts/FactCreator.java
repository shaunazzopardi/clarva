package clarva.analysis.facts;

public interface FactCreator<Statement> {
    public Facts extract(Statement statement);
}
