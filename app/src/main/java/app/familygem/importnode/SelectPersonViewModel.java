package app.familygem.importnode;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SelectPersonViewModel extends ViewModel {
    private final MutableLiveData<String> personId = new MutableLiveData<String>();

    public enum State{
        NONE,
        SELECT_PERSON_1,
        SELECT_PERSON_2
    }

    private State state = State.NONE;
    private String personId1;
    private String personId2;

    public void setPersonId(String personId){
        this.personId.setValue(personId);
    }

    public LiveData<String> getPersonId(){
        return personId;
    }

    public void setState(State state){
        this.state = state;
    }

    public State getState(){
        return state;
    }
}
