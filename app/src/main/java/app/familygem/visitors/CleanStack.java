// Strettamente connesso a FindStack, individua gli oggetti da tenere nella pila

package app.familygem.visitors;

class CleanStack extends TotalVisitor {

	private Object target;
	boolean toDelete = true;

	CleanStack(Object target) {
		this.target = target;
	}

	@Override
	boolean visitObject(Object object, boolean isRoot) { // il boolean qui è inutilizzato
		if (object.equals(target))
			toDelete = false;
		return true;
	}
}
