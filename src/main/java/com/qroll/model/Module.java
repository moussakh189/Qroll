package com.qroll.model;

import java.util.Objects;

public class Module {
    private String moduleCode ;
    private String moduleName ;
    private int semester ;


    public Module()
    {
    }

    public Module(String moduleCode , String moduleName , int semester)
    {
        this.moduleCode = moduleCode ;
        this.moduleName = moduleName ;
        this.semester = semester ;
    }

    public String getModuleCode() {return moduleCode;
    }
    public int getSemester() { return semester;
    }
    public String getModuleName() {return moduleName;}



    public void setModuleCode(String moduleCode)  { this.moduleCode = moduleCode; }
    public void setModuleName(String moduleName)  { this.moduleName = moduleName; }
    public void setSemester(int semester)         { this.semester   = semester; }


    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true ;
        if(!(obj instanceof  Module)) return false ;

        return Objects.equals(moduleCode, ((Module) obj ).moduleCode);
    }

    @Override
    public int hashCode() {
        return  Objects.hash(moduleCode);
    }

    @Override
    public String toString() {
        return String.format("Module{code='%s', name='%s', semester=%d}", moduleCode, moduleName, semester);
    }
}
