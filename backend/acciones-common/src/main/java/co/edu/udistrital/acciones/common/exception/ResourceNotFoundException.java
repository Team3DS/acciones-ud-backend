package co.edu.udistrital.acciones.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    private final String resourceType, resourceId;
    public ResourceNotFoundException(String resourceType, String resourceId) { super(resourceType + " not found: " + resourceId); this.resourceType = resourceType; this.resourceId = resourceId; }
}